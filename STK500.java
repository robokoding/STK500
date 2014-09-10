import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;

public class STK500 {
    private OutputStream outStream = null;
    private InputStream inputStream = null;

    private static void openStreams() {
        try {
            /* TODO: implement opening bluetooth */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void closeStreams() {
        try {
            /* TODO: implement closing bluetooth */
            outStream.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] readProgram() {
        FileInputStream fis = null;
        File file = new File("/sdcard/sumorobot/main.hex");

        // every line, except last one, has has 45 bytes (including \r\n)
        int programLines = (int) Math.ceil(file.length() / 45.0);
        // every line has 32 bytes of program data (excluding checksums, addresses, etc.)
        int unusedBytes = 45 - 32;
        // calculate program length according to program lines and unused bytes
        int programLength = (int) file.length() - (programLines * unusedBytes);
        // the actualy program data is half the size, as the hex file represents hex data in individual chars
        programLength /= 2;
        // create a byte array with the program length
        byte[] program = new byte[programLength];

        try {
            // open the file stream
            log.logcat("opening hex file", "d");
            fis = new FileInputStream(file);
            log.logcat("Total program size (in bytes) : " + programLength, "d");
            log.logcat("Total file size to read (in bytes) : " + fis.available(), "d");

            int content;
            int lineIndex = 0;
            int lineNumber = 1;
            int programIndex = 0;
            char[] line = new char[45];
            // read the file byte by byte
            while ((content = fis.read()) != -1) {
                // append byte to the line
                line[lineIndex++] = (char) content;
                // when the line is complete
                if (content == 10) {
                    // take only the actual program data form the line
                    for (int index = 9; index < lineIndex - 4; index += 2) {
                        // convert hexadecimals represented as chars into bytes
                        program[programIndex++] = Integer.decode("0x" + line[index] + line[index+1]).byteValue();
                    }
                    // start a new line
                    lineIndex = 0;
                }
            }
        } catch (IOException e) {
            log.logcat("reading hex failed: " + e.getMessage(), "d");
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return program;
    }

    public static void main (String[] args) {
        byte[] program = readProgram();
        System.out.println("program length: " + program.length);

        System.out.println("opening streams");
        openStreams();

        System.out.println("syncing");
        for (int i = 0; i < 5; i++) {
            outStream.write(0x30);
            outStream.write(0x20);
            try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}
        }

        System.out.println("waiting for response");
        int insync = inputStream.read();
        int ok = inputStream.read();
        if (insync == 0x14 && ok == 0x10) {
            System.out.println("insync");
        }

        System.out.println("reading major version");
        outStream.write(0x41);
        outStream.write(0x81);
        outStream.write(0x20);
        try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

        System.out.println("waiting for response");
        insync = inputStream.read();
        int major = inputStream.read();
        ok = inputStream.read();
        if (insync == 0x14 && ok == 0x10) {
            System.out.println("insync", "d");
        }

        System.out.println("reading minor version");
        outStream.write(0x41);
        outStream.write(0x82);
        outStream.write(0x20);
        try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

        System.out.println("waiting for response");
        insync = inputStream.read();
        int minor = inputStream.read();
        ok = inputStream.read();
        if (insync == 0x14 && ok == 0x10) {
            System.out.println("insync");
        }

        System.out.println("version: " + major + "." + minor);

        System.out.println("entering programming mode");
        outStream.write(0x50);
        outStream.write(0x20);
        try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

        System.out.println("waiting for response");
        insync = inputStream.read();
        ok = inputStream.read();
        if (insync == 0x14 && ok == 0x10) {
            System.out.println("insync");
        }

        System.out.println("getting device signature");
        outStream.write(0x75);
        outStream.write(0x20);
        try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

        System.out.println("waiting for response");
        insync = inputStream.read();
        byte [] signature = new byte[3];
        inputStream.read(signature, 0, 3);
        ok = inputStream.read();
        if (insync == 0x14 && ok == 0x10) {
            System.out.println("insync");
        }

        System.out.println("signature: " + signature[0] + "." + signature[1] + "." + signature[2]);

        int size = 0;
        int address = 0;
        int programIndex = 0;
        while (true) {
            int laddress = address % 256;
            int haddress = address / 256;
            address += 64;

            System.out.println("loading page address");
            outStream.write(0x55);
            outStream.write(laddress);
            outStream.write(haddress);
            outStream.write(0x20);
            try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

            System.out.println("waiting for response");
            insync = inputStream.read();
            ok = inputStream.read();
            if (insync == 0x14 && ok == 0x10) {
                System.out.println("insync");
            }

            if (program.length - programIndex < 128) {
                size = program.length - programIndex;
            } else {
                size = 128;
            }
            System.out.println("programming page size: " + size + " haddress: " + haddress + " laddress: " + laddress);
            outStream.write(0x64);
            outStream.write(0x00);
            outStream.write(size);
            outStream.write(0x46);
            for (int i = 0; i < size; i++) {
                outStream.write(program[programIndex++]);
            }
            outStream.write(0x20);
            try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

            System.out.println("receiving sync ack");
            insync = inputStream.read();
            ok = inputStream.read();

            if (insync == 0x14 && ok == 0x10) {
                System.out.println("insync");
            }

            if (size != 0x80) {
                break;
            }
        }
        System.out.println("program index: " + programIndex);

        System.out.println("leaving programming mode");
        outStream.write(0x51);
        outStream.write(0x20);
        try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

        System.out.println("receiving sync ack");
        insync = inputStream.read();
        ok = inputStream.read();

        if (insync == 0x14 && ok == 0x10) {
            System.out.println("insync");
        }

        System.out.println("closing streams");
        closeStreams();
    }
}