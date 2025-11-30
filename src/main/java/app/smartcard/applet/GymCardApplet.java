package app.smartcard.applet;

import javacard.framework.*;

/**
 * Gym Card Applet - Java Card Applet lưu thông tin thành viên gym
 *
 * Cấu trúc dữ liệu trên thẻ:
 * - Member ID (10 bytes)
 * - Full Name (50 bytes)
 * - Package Type (20 bytes)
 * - Balance (8 bytes - long)
 * - Start Date (10 bytes)
 * - Expire Date (10 bytes)
 *
 * Command APDUs:
 * - VERIFY PIN: 00 20 00 00 04 [4 bytes PIN]
 * - READ DATA: 00 30 00 00 00
 * - UPDATE BALANCE: 00 40 00 00 08 [8 bytes balance] (requires PIN)
 * - CHANGE PIN: 00 50 00 00 08 [4 old][4 new]
 * - SET DATA: 00 60 00 00 6C [108 bytes] (requires PIN)
 */
public class GymCardApplet extends Applet {

    // Command codes
    private static final byte INS_VERIFY_PIN = (byte) 0x20;
    private static final byte INS_READ_DATA = (byte) 0x30;
    private static final byte INS_UPDATE_BALANCE = (byte) 0x40;
    private static final byte INS_CHANGE_PIN = (byte) 0x50;
    private static final byte INS_SET_DATA = (byte) 0x60;

    // Status codes
    private static final short SW_PIN_VERIFICATION_REQUIRED = 0x6300;
    private static final short SW_PIN_TRIES_REMAINING = 0x63C0;

    // Data offsets and sizes
    private static final short OFFSET_MEMBER_ID = 0;
    private static final short OFFSET_FULL_NAME = 10;
    private static final short OFFSET_PACKAGE_TYPE = 60;
    private static final short OFFSET_BALANCE = 80;
    private static final short OFFSET_START_DATE = 88;
    private static final short OFFSET_EXPIRE_DATE = 98;

    private static final short SIZE_MEMBER_ID = 10;
    private static final short SIZE_FULL_NAME = 50;
    private static final short SIZE_PACKAGE_TYPE = 20;
    private static final short SIZE_BALANCE = 8;
    private static final short SIZE_DATE = 10;
    private static final short TOTAL_DATA_SIZE = 108;

    // PIN configuration
    private static final byte PIN_TRY_LIMIT = 3;
    private static final byte PIN_SIZE = 4;

    // Storage
    private byte[] cardData;
    private OwnerPIN pin;

    /**
     * Constructor - private, called by install()
     */
    private GymCardApplet() {
        // Allocate persistent memory
        cardData = new byte[TOTAL_DATA_SIZE];

        // Initialize PIN (default: 1234)
        byte[] defaultPin = {(byte)'1', (byte)'2', (byte)'3', (byte)'4'};
        pin = new OwnerPIN(PIN_TRY_LIMIT, PIN_SIZE);
        pin.update(defaultPin, (short)0, PIN_SIZE);

        // Clear card data
        Util.arrayFillNonAtomic(cardData, (short)0, TOTAL_DATA_SIZE, (byte)0);
    }

    /**
     * Install method - called when applet is loaded onto card
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new GymCardApplet().register();
    }

    /**
     * Process incoming APDUs
     */
    public void process(APDU apdu) {
        // SELECT command is handled automatically
        if (selectingApplet()) {
            return;
        }

        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[ISO7816.OFFSET_INS];

        switch (ins) {
            case INS_VERIFY_PIN:
                verifyPIN(apdu);
                break;
            case INS_READ_DATA:
                readData(apdu);
                break;
            case INS_UPDATE_BALANCE:
                updateBalance(apdu);
                break;
            case INS_CHANGE_PIN:
                changePIN(apdu);
                break;
            case INS_SET_DATA:
                setData(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    /**
     * Verify PIN
     */
    private void verifyPIN(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();

        if (bytesRead != PIN_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        if (pin.check(buffer, ISO7816.OFFSET_CDATA, PIN_SIZE)) {
            // PIN correct
            return;
        } else {
            // PIN incorrect
            byte triesRemaining = pin.getTriesRemaining();
            if (triesRemaining == 0) {
                ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
            } else {
                ISOException.throwIt((short)(SW_PIN_TRIES_REMAINING | triesRemaining));
            }
        }
    }

    /**
     * Read all card data (no PIN required)
     */
    private void readData(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        // Copy data to buffer
        Util.arrayCopyNonAtomic(cardData, (short)0, buffer, (short)0, TOTAL_DATA_SIZE);

        // Send response
        apdu.setOutgoingAndSend((short)0, TOTAL_DATA_SIZE);
    }

    /**
     * Update balance (requires PIN)
     */
    private void updateBalance(APDU apdu) {
        if (!pin.isValidated()) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();

        if (bytesRead != SIZE_BALANCE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Update balance
        Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA,
                                cardData, OFFSET_BALANCE, SIZE_BALANCE);
    }

    /**
     * Change PIN
     */
    private void changePIN(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();

        if (bytesRead != (PIN_SIZE * 2)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Verify old PIN
        if (!pin.check(buffer, ISO7816.OFFSET_CDATA, PIN_SIZE)) {
            byte triesRemaining = pin.getTriesRemaining();
            if (triesRemaining == 0) {
                ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
            } else {
                ISOException.throwIt((short)(SW_PIN_TRIES_REMAINING | triesRemaining));
            }
            return;
        }

        // Update to new PIN
        pin.update(buffer, (short)(ISO7816.OFFSET_CDATA + PIN_SIZE), PIN_SIZE);
    }

    /**
     * Set all card data (requires PIN)
     */
    private void setData(APDU apdu) {
        if (!pin.isValidated()) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        short bytesRead = apdu.setIncomingAndReceive();

        if (bytesRead != TOTAL_DATA_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        // Set all data
        Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA,
                                cardData, (short)0, TOTAL_DATA_SIZE);
    }
}
