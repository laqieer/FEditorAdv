/*
 * CREATED BY CAMTECH075
 *
 * Meat and bones.
 */

package Graphics;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.Graphics;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.*;

//import camtech.util.StringOutputHelper;
//import static camtech.util.StringOutputHelper.toHex;

public class ScriptReader {
    static final int DATA_SIZE = 4;
    // BwdYeti: the elements of these can be changed because of how final works,
    // but I'm making them final anyway just to at least fix the length, someone
    // else can make it truly immutable if needed
    static final String[] MODE_NAMES = { "All", "Attack", "", "Crit", "",
        "Ranged", "RangedCrit", "Avoid", "AvoidRanged",
        "Idle", "Idle2", "IdleRanged", "AttackMiss"};
    static final int[] HIT_HOLD_TIMES = {
        15, 0, 15, 0, 25, 25, 25, 25, 0, 0, 0, 8 };
    static final int THROW_AXE_HOLD_TIME = 24;

    private PrintStream dataOutput = new PrintStream("output.txt");
    private boolean[] modesRipped;
    private byte[] frameData;

    public static final byte FRAME_COMMAND = (byte) 0x86;
    public static final byte TERMINATOR = (byte) 0x80;
    public static final byte CALL_COMMAND = (byte) 0x85;

    private boolean indexOutOfRange(int index, int dataSize)
    {
        return index >= frameData.length / dataSize;
    }
    private boolean readData(byte[] bytes, int index)
    {
        if (indexOutOfRange(index, bytes.length))
            return false;
        System.arraycopy(frameData, index * bytes.length, bytes, 0, bytes.length);
        return true;
    }

    public void readScript(String writeToPath) throws IOException {

        PrintStream output = new PrintStream(writeToPath);

        int modeNum = 1;

        output.println("//begin Mode 1");

        byte[] word = new byte[DATA_SIZE];
        for(int index = 0; modeNum <= 12 && !indexOutOfRange(index, word.length); index++)
        {
            readData(word, index);

            switch(word[3])
            {
                    case TERMINATOR:
                    output.println("~~~");
                    System.out.println("Ripped mode: " + modeNum++);

                    if (modeNum < 13) { output.println("//begin Mode " + modeNum); }
                    else { output.print("//End of animation script"); }
                break;

                case CALL_COMMAND:
                    if (!modesRipped[modeNum-1]) { continue; }

                    if (word[0] < 0x10) { output.print("C0"); }
                    else { output.print("C"); }

                    output.println(toHex(word[0]));
                break;

                case FRAME_COMMAND:
                    if (!modesRipped[modeNum-1]) { continue; }
                    output.print(word[0] + "  p- ");

                    // BwdYeti: these differentiations aren't actually helpful!
                    /*switch(modeNum)
                    {
                            case 1:
                            case 2:
                            case 12:
                                    frameName = "att-";
                            break;

                            case 3:
                            case 4:
                                    frameName = "crt-";
                            break;

                            case 5:
                                    frameName = "rng-";
                            break;

                            case 6:
                                    frameName = "rngCrit-";
                            break;

                            case 7:
                            case 8:
                                    frameName = "dodge-";
                            break;

                            case 9:
                            case 10:
                            case 11:
                                    frameName = "stand-";
                            break;

                            default:
                                    frameName = "?-";
                            break;
                    }*/
                            output.println(String.format("frame%03d.png", word[2] & 0xFF));
                break;

                case 0x00: break;

                default:
                    output.println("//Unknown command: " +
                            toHex(word[0]) + " " + toHex(word[1]) + " " + toHex(word[2]) + " " + toHex(word[3]));
                break;
            }
        }
        output.close();
        return;

    }

    public ScriptReader (
            byte[] frameData)
            throws IOException {
        this(frameData, true, true, true, true, true, true);
    }
    public ScriptReader (
            byte[] frameData,
            boolean ripAttack,
            boolean ripCrit,
            boolean ripRange,
            boolean ripRngCrit,
            boolean ripDodge,
            boolean ripStand )
            throws IOException {

            this.frameData = frameData;

            this.modesRipped = new boolean[12];
            this.modesRipped[0] = this.modesRipped[1] = this.modesRipped[11] = ripAttack; //1,2,12
            this.modesRipped[2] = this.modesRipped[3] = ripCrit; //3,4
            this.modesRipped[4] = ripRange; //5
            this.modesRipped[5] = ripRngCrit; //6
            this.modesRipped[6] = this.modesRipped[7] = this.modesRipped[8] = ripDodge; //7,8,9
            this.modesRipped[9] = this.modesRipped[10] = ripStand; //10,11

    }

    public void dumpGif(String writePath, BufferedImage[] images) throws Exception {
        if (images.length > 0) {
            int width = 0, height = 0, type = -1;
            for(int i = 0; i < images.length; i++)
                if (images[i] != null) {
                    width = images[i].getWidth();
                    height = images[i].getHeight();
                    type = images[i].getType();
                    break;
                }
            if (width == 0 || height == 0)
                return;

            int modeNum = 0, index = 0;

            byte[] word = new byte[DATA_SIZE];
            while(modeNum <= 12 && !indexOutOfRange(index, word.length))
            {
                // If this mode is skipped, advance the index until the next terminator is found
                // Attack/crit lower layers (modes 2 and 4) are not handled as their own animations
                if (modeNum > 0 && (!modesRipped[modeNum-1] || modeNum == 2 || modeNum == 4)) {
                    while (readData(word, index++) && word[3] != TERMINATOR){}
                    modeNum++;
                    continue;
                }
                
                ImageOutputStream output = new FileImageOutputStream(
                        new File(writePath + String.format("%02d%s.gif", modeNum, MODE_NAMES[modeNum])));
                GifSequenceWriter writer = new GifSequenceWriter(output, type, 0, true);

                index = writeGifMode(writer, images, modeNum, index, word, width, height);
                writer.close();
                output.close();
                modeNum++;
            }
        }
    }
    private int writeGifMode(
            GifSequenceWriter writer,
            BufferedImage[] images,
            int modeNum,
            int index,
            byte[] word,
            int width,
            int height) throws IOException {
        int actualModeNum = modeNum == 0 ? 1 : modeNum;
        
        // For the attack and crit modes, there is also the lower layer to worry about
        byte[] lowerWord = new byte[DATA_SIZE];
        int lowerIndex;
        if (actualModeNum == 1 || actualModeNum == 3)
            lowerIndex = getStartOfNextMode(actualModeNum, index, lowerWord);
        else
            lowerIndex = -1;
        boolean firstFrame = true;
        int time1 = 0, time2 = 0;
        
        while(readData(word, index)) {
            switch(word[3])
            {
                case TERMINATOR:
                    System.out.println("Ripped mode: " + actualModeNum);
                    index++;
                    if (modeNum != 0)
                        return index;
                    else {
                        actualModeNum++;
                        if (actualModeNum > 12)
                            return 0;
                        
                        // Skip attack/crit lower layer
                        while (modeNum == 0 && (actualModeNum == 2 || actualModeNum == 4)) {
                            index = getStartOfNextMode(actualModeNum, index, word);
                            actualModeNum++;
                        }
                    
                        if (actualModeNum == 1 || actualModeNum == 3)
                            lowerIndex = getStartOfNextMode(actualModeNum, index, lowerWord);
                        else
                            lowerIndex = -1;
                        firstFrame = true;
                        time1 = 0;
                        time2 = 0;
                        break;
                    }

                case FRAME_COMMAND:
                    int frame = word[2] & 0xFF;
                    int ticks = (word[0] & 0xFF - time1);
                    
                    for(;;) {
                        // Advances lower index, if it exists
                        if (lowerIndex != -1) 
                            for(;;) {
                                if (!readData(lowerWord, lowerIndex)){
                                    lowerIndex = -1;
                                    break;
                                }
                                if (lowerWord[3] == FRAME_COMMAND)
                                    break;
                                lowerIndex++;
                        }
                        int lowerFrame = lowerIndex == -1 ? -1 : (lowerWord[2] & 0xFF);
                        int lowerTicks = lowerIndex == -1 ? -1 : (lowerWord[0] & 0xFF);
                        // Determines the time of this current frame, counting both layers
                        int actualTicks;
                        if (lowerIndex == -1)
                            actualTicks = ticks;
                        else
                            actualTicks = Math.min(ticks - time1, lowerTicks - time2);
                        // Check the following commands to see if any are wait for hp depletion
                        int hitPauseTime = 0;
                        if (time1 + actualTicks >= ticks)
                            for(int i = 1; readData(word, index + i); i++) {
                                if (word[3] == 0x00)
                                    continue;
                                if (word[3] != CALL_COMMAND)
                                    break;
                                // 0x01 is wait for hp depletion
                                if (word[0] == 0x01)
                                    hitPauseTime = Math.max(
                                            hitPauseTime, HIT_HOLD_TIMES[actualModeNum - 1]);
                                // 0x13 is wait for throw axe or something
                                if (word[0] == 0x13)
                                    hitPauseTime = Math.max(
                                            hitPauseTime, THROW_AXE_HOLD_TIME);
                            }

                        writeGifImage(writer, frame > images.length ? null : images[frame],
                                lowerFrame == -1 || lowerFrame > images.length ? null : images[lowerFrame],
                                actualTicks + (firstFrame ? 30 : 0), width, height);
                        if (hitPauseTime > 0)
                            writeGifImage(writer, frame > images.length ? null : images[frame],
                                    lowerFrame == -1 || lowerFrame > images.length ? null : images[lowerFrame],
                                    hitPauseTime, width, height);

                        firstFrame = false;
                        time1 += actualTicks;
                        time2 += actualTicks;
                        if (lowerIndex != -1 && time2 >= lowerTicks){
                            lowerIndex++;
                            time2 = 0;
                        }
                        if (time1 >= ticks){
                            time1 = 0;
                            break;
                        }
                    }
                    index++;
                break;

                default:
                    index++;
                    break;
            }
        }
        return index + 1;
    }
    private int getStartOfNextMode(int modeNum, int index, byte[] word) {
        int nextIndex = -1;
        // Starts with the position in the current mode, then finds the start of the next mode
        nextIndex = index;
        while(readData(word, nextIndex)){
            if (word[3] == TERMINATOR){
                nextIndex++;
                break;
            }
            nextIndex++;
        }
        if (!readData(word, nextIndex))
            nextIndex = -1;
        return nextIndex;
    }
    private void writeGifImage(
            GifSequenceWriter writer,
            BufferedImage upperImg,
            BufferedImage lowerImg,
            int ticks,
            int width,
            int height) throws IOException {
        int delayMS = (int)Math.round((ticks * 100.0) / 60) * 10;
        BufferedImage tempImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = tempImg.getGraphics();
        if (lowerImg != null)
            g.drawImage(lowerImg, 0, 0, null);
        if (upperImg != null)
            g.drawImage(upperImg, 0, 0, null);
        g.dispose();
        writer.writeToSequence(tempImg, delayMS);
    }

    public static String toHex(long a) {

        String result = Long.toString(a, 16);

        return result.toUpperCase();

    }
}
