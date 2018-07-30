package algorithm;

//import android.util.Log;
//import li.power.idsl.most.app.sensor.Sensor;

import com.company.Main;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by PowerLi on 2017/5/21.
 */

/**
 * Version control by Jimmy on 2018/5/15
 * note, byte -> 8 bits , int -> 32 bits
 * getSha will hash input byte[], int, string,then return to bytes
 * doing logic operation needs to transfer to int
 * getXORWithSha will get the last 4 bytes of SHA_BYTE_SIZE and perform xor with the int variable
 * getSubSha turn byte into "int", in a brutal way(Shifting)
 */
public class Algorithm {

    public static final byte[] SK_SN = {(byte) 0xaa, 0x56, 0x52, (byte) 0x85, 0x6d, 0x59, 0x1a, (byte) 0xbe, (byte) 0x89, (byte) 0xbd, (byte) 0xf0, 0x43, (byte) 0x8f, (byte) 0xe5, 0x7f, 0x60, 0x16, 0x41, (byte) 0xae, (byte) 0xda};
    public static final byte[] SHA_SK_SN = {0x3b, (byte) 0xf5, 0x32, 0x7c, (byte) 0xcf, (byte) 0xe0, 0x2a, 0x67, (byte) 0xe8, 0x6f, 0x41, (byte) 0xc4, 0x5f, 0x59, (byte) 0x0d, (byte) 0xd1, (byte) 0xa8, 0x4c, 0x5d, 0x55};

    //Pass the "token" and the "m" to the next class
    public static int m;
    public static byte[] tksni;
    public static int[] phase1(String name, byte[] data) {

        int idsn = ((data[5] & 0xff) << 24) + ((data[4] & 0xff) << 16) + ((data[3] & 0xff) << 8) + (data[2] & 0xff);
        int r1 = ((data[9] & 0xff) << 24) + ((data[8] & 0xff) << 16) + ((data[7] & 0xff) << 8) + (data[6] & 0xff);
        byte[] mt = Arrays.copyOfRange(data, 10, 30);
        byte[] x = Arrays.copyOfRange(data, 30, 50);
        byte[] m1 = Arrays.copyOfRange(data, 50, 70);
        byte[] m2 = Arrays.copyOfRange(data, 70, 90);

        Log.d(name + "-AUTH", "idsn:\t" + idsn);

        Log.d(name + "-AUTH", "r1:\t" + r1);

        Log.d(name + "-AUTH", " x:\t" + Hash.bytesToHex(x));

        Log.d(name + "-AUTH", "mt:\t" + Hash.bytesToHex(mt));

        Log.d(name + "-AUTH", "m1s:\t" + Hash.bytesToHex(m1));

        Log.d(name + "-AUTH", "m2s:\t" + Hash.bytesToHex(m2));

        byte[] m2p = Hash.getHmac(SK_SN, idsn, x, m1, r1, mt);
        Log.d(name + "-AUTH", "m2p:\t" + Hash.bytesToHex(m2p));


        if (!Hash.compareByteArray(m2, m2p)) {
            Log.d(name + "-AUTH", "m2 mismatch");
            return null;
        }

        byte[] mbp = Hash.getSHA(Hash.getXorWithSHA(SK_SN, r1));
        Log.d(name + "-AUTH", "mtp:\t" + Hash.bytesToHex(mbp));

        int rb = Hash.getSubSHA(mt) ^ Hash.getSubSHA(mbp);
        Log.d(name + "-AUTH", "ts:\t" + String.valueOf(rb));

        byte[] rbHash = Hash.getSHA(rb);
        int v = Hash.getSubSHA(rbHash) ^ Hash.getSubSHA(x);

        byte[] m1p = Hash.getSHA(Hash.getXorWithSHA(SHA_SK_SN, (idsn | v)));
        Log.d(name + "-AUTH", "m1p:\t" + Hash.bytesToHex(m1p));

        if (!Hash.compareByteArray(m1, m1p))
            return null;

        int[] result = new int[3];
        result[0] = idsn;
        result[1] = v;
        result[2] = r1;

        return result;
    }

    public static byte[] phase2(String name, int idsn, int v, int r1) {

        //Sensor.previousTime = System.currentTimeMillis(); //Set Static Authentication start time

        Random random = new Random();
        int w = 0x01000000 + random.nextInt(0x0fffffff);
        int n1 = 0x01000000 + random.nextInt(0x0fffffff);
        Log.d(name + "-AUTH", "  w:\t" + String.valueOf(w));
        //Log.d(name + "-AUTH", " ni:\t" + String.valueOf(w));

        byte[] y = Hash.getXorWithSHA(Hash.getSHA(Hash.getXorWithSHA(SK_SN, n1)), w);
        Log.d(name + "-AUTH", "  y:\t" + Hash.bytesToHex(y));

        tksni = Hash.getSHA(Hash.getXorWithSHA(SK_SN, v ^ w)); //Store tksni
        Log.d(name + "-AUTH", "tksni:\t" + Hash.bytesToHex(tksni));

        byte[] m3 = Hash.getSHA(Hash.getXorWithSHA(SHA_SK_SN, Hash.getSubSHA(tksni) | idsn));
        Log.d(name + "-AUTH", " m3:\t" + Hash.bytesToHex(m3));
        byte[] m4 = Hash.getHmac(SK_SN, n1, r1, y, m3);
        Log.d(name + "-AUTH", " m4:\t" + Hash.bytesToHex(m4));

        Log.d(name + "-AUTH", " w:\t" + w);
        m = w; //set m equals w
        Log.d(name + "-AUTH", " m:\t" + m);

        byte[] toSensor = new byte[66];
        byte[] n1Bytes = ByteBuffer.allocate(4).putInt(n1).array();
        toSensor[0] = 0x66;
        toSensor[1] = (byte) 0x99;
        for (int i = 0; i < 20; i++) //uin8_t size 20
        {
            toSensor[i + 2] = m3[i];
            toSensor[i + 20 + 2] = m4[i];
            toSensor[i + 40 + 2] = y[i];
        }
        for (int i = 0; i < 4; i++) {
            toSensor[i + 60 + 2] = n1Bytes[i];
        }

        return toSensor;
    }

    public static byte[] contPhase(String name, byte[] data, boolean entStatic) {

        //data[0] = 0x77, data=0x88, for entering the continuous authentication
        int idsn = ((data[5] & 0xff) << 24) + ((data[4] & 0xff) << 16) + ((data[3] & 0xff) << 8) + (data[2] & 0xff);
        int r2 = ((data[9] & 0xff) << 24) + ((data[8] & 0xff) << 16) + ((data[7] & 0xff) << 8) + (data[6] & 0xff);
        byte[] mt = Arrays.copyOfRange(data, 10, 30);
        byte[] ms = Arrays.copyOfRange(data, 30, 50);
        byte[] m5 = Arrays.copyOfRange(data, 50, 70);

        Log.d(name + "-AUTH", "idsn:\t" + idsn);

        Log.d(name + "-AUTH", "r2:\t" + r2);

        Log.d(name + "-AUTH", "mt:\t" + Hash.bytesToHex(mt));

        Log.d(name + "-AUTH", "ms:\t" + Hash.bytesToHex(ms));

        Log.d(name + "-AUTH", "m5:\t" + Hash.bytesToHex(m5));

        Log.d(name + "-AUTH", " m:\t" + m);

        Log.d(name + "-AUTH", "tksni:\t" + Hash.bytesToHex(tksni));

        int tc_snp;
        byte[] ack = null; //These two are also popular
        byte[] y1;

        if (entStatic) {
            //generate tc_snp
            System.out.println("Next phase static authentication");
            //tc_sn
            byte[] ts = ByteBuffer.allocate(4).putInt(Hash.getSubSHA(tksni)).array();
            byte[] mr2 = ByteBuffer.allocate(4).putInt(m ^ r2).array();
            for (int i = 0; i < 4; i++)
                ts[i] = (byte) (ts[i] | mr2[i]);

            byte[] tc_tmp = Hash.getSHA(ts);
            System.out.println("Start parameter generator");
            tc_snp = Hash.getSubSHA(tc_tmp) ^ Hash.getSubSHA(mt);
            Log.d(name + "-AUTH", "ts:\t" + tc_snp);

            //generate y1
            byte[] y1_temp1 = Hash.getXorWithSHA(tksni, r2); //tksni^r2

            int y1_temp1p = Hash.getSubSHA(y1_temp1);

            byte[] tcr2 = ByteBuffer.allocate(4).putInt(y1_temp1p).array();

            Log.d(name + "-AUTH", " m:\t" + m);

            byte[] m_temp = ByteBuffer.allocate(4).putInt(m).array();
            for (int i = 0; i < 4; i++)
                tcr2[i] = (byte) (tcr2[i] | m_temp[i]);

            byte[] pre_y = Hash.getSHA(tcr2);


            int y1_temp4 = m | Hash.getSubSHA(tksni); //next is H(m|tksni)
            Log.d(name + "-AUTH", "m|tk:\t" + y1_temp4);

            y1 = Hash.getXorWithSHA(pre_y, y1_temp4);
            Log.d(name + "-AUTH", "y1:\t" + Hash.bytesToHex(y1));

            //generate ack
            Log.d(name + "-AUTH", " m:\t" + m);

            byte[] tmp_1 = ByteBuffer.allocate(4).putInt(m ^ tc_snp).array();
            byte[] tmp_2 = ByteBuffer.allocate(4).putInt(tc_snp ^ r2).array();
            byte[] tmp_3 = ByteBuffer.allocate(4).putInt(m | Hash.getSubSHA(tksni)).array();
            for (int i = 0; i < 4; i++) {
                tmp_1[i] = (byte) (tmp_1[i] | tmp_2[i] | tmp_3[i]);
        }
            ack = Hash.getSHA(tmp_1);
            Log.d(name + "-AUTH", "ack:\t" + Hash.bytesToHex(ack));

        } else {
            //generate tc_snp
            System.out.println("Next phase Continuous authentication");

            //generate tc_sn
            byte[] ts = ByteBuffer.allocate(4).putInt(Hash.getSubSHA(tksni)).array();
            byte[] mr2 = ByteBuffer.allocate(4).putInt(m ^ r2).array();
            for (int i = 0; i < 4; i++)
                ts[i] = (byte) (ts[i] | mr2[i]);

            byte[] tc_tmp = Hash.getSHA(ts);

            System.out.println("Start parameter generator");
            tc_snp = Hash.getSubSHA(tc_tmp) ^ Hash.getSubSHA(mt);
            Log.d(name + "-AUTH", "tc:\t" + tc_snp);

            //generate sdp, though it wasn't used by the way.
            byte[] sdp_tmp1 = Hash.getXorWithSHA(tksni, m);
            byte[] sdp_tmp2 = Hash.getSHA(Hash.getSubSHA(sdp_tmp1) | r2);
            byte[] sdp = Hash.getXorWithSHA(sdp_tmp2, Hash.getSubSHA(ms));
            Log.d(name + "-AUTH", "sd':\t" + Hash.bytesToHex(sdp));

            //generate M5'
            byte[] m5p = Hash.getHmac(SK_SN, idsn, ms, mt, r2);
            Log.d(name + "-AUTH", "m5p:\t" + Hash.bytesToHex(m5p));
            if (!Hash.compareByteArray(m5, m5p)) {
                Log.d(name + "-AUTH", "m5 mismatch");
                Main.authed = false;
                return null;
            }// if authed is confirmed, continue and set authed = true
            Random random = new Random();
            int n2 = 0x01000000 + random.nextInt(0x0fffffff);
            //generate y1

            byte[] y1_temp1 = Hash.getXorWithSHA(tksni, r2); //tksni^r2

            int y1_temp1p = Hash.getSubSHA(y1_temp1);

            byte[] tcr2 = ByteBuffer.allocate(4).putInt(y1_temp1p).array();

            Log.d(name + "-AUTH", " m:\t" + m);

            byte[] m_temp = ByteBuffer.allocate(4).putInt(m).array();
            for (int i = 0; i < 4; i++)
               tcr2[i] = (byte) (tcr2[i] | m_temp[i]);

            byte[] pre_y = Hash.getSHA(tcr2);

            y1 = Hash.getXorWithSHA(pre_y, n2);
            Log.d(name + "-AUTH", "y1:\t" + Hash.bytesToHex(y1));
            Log.d(name + "-AUTH", " m:\t" + m);


            byte[] tmp_1 = ByteBuffer.allocate(4).putInt(m ^ tc_snp).array();
            byte[] tmp_2 = ByteBuffer.allocate(4).putInt(n2 ^ r2).array();
            byte[] tmp_3 = ByteBuffer.allocate(4).putInt(m ^ Hash.getSubSHA(tksni)).array();
            for (int i = 0; i < 4; i++) {
                tmp_1[i] = (byte) (tmp_1[i] | tmp_2[i] | tmp_3[i]);

            }
            ack = Hash.getSHA(tmp_1);
            Log.d(name + "-AUTH", "ack:\t" + Hash.bytesToHex(ack));

            //set m
            m = n2;
            Log.d(name + "-AUTH", "n2:\t" + m);
            //System.out.printf("%X",n2);
        }

        byte[] toSensor = new byte[42];
        toSensor[0] = 0x66;
        toSensor[1] = (byte) 0x99;
        for (int i = 0; i < 20; i++) {
            toSensor[2 + i] = y1[i];
            toSensor[2 + 20 + i] = ack[i];
        }
        return toSensor;
    }

}
