/*
 * CREATED BY CAMTECH075
 *
 * Meat and bones.
 */

package Model;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.IOException;

//import camtech.util.StringOutputHelper;
//import static camtech.util.StringOutputHelper.toHex;

public class ScriptReader {

	private PrintStream dataOutput = new PrintStream("output.txt");
	private FileInputStream reader;
	private boolean[] modesRipped;

	public static final byte FRAME_COMMAND = (byte) 0x86;
	public static final byte TERMINATOR = (byte) 0x80;
	public static final byte CALL_COMMAND = (byte) 0x85;

	public void readScript(String writeToPath) throws IOException {

		PrintStream output = new PrintStream(writeToPath);

		//int countVar = 0;
		int modeNum = 1;

		output.println("//begin Mode 1");

		while(modeNum < 13 && reader.available() > 0)
		{
			byte[] word = new byte[4];
			reader.read(word);

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

		return;

	}

	public ScriptReader (
                String path)
                throws IOException {
            this(path, true, true, true, true, true, true);
        }
	public ScriptReader (
		String path,
		boolean ripAttack,
		boolean ripCrit,
		boolean ripRange,
		boolean ripRngCrit,
		boolean ripDodge,
		boolean ripStand )
		throws IOException {

		reader = new FileInputStream(new File(path));

		this.modesRipped = new boolean[12];
		this.modesRipped[0] = this.modesRipped[1] = this.modesRipped[11] = ripAttack; //1,2,12
		this.modesRipped[2] = this.modesRipped[3] = ripCrit; //3,4
		this.modesRipped[4] = ripRange; //5
		this.modesRipped[5] = ripRngCrit; //6
		this.modesRipped[6] = this.modesRipped[7] = this.modesRipped[8] = ripDodge; //7,8,9
		this.modesRipped[9] = this.modesRipped[10] = ripStand; //10,11

	}

	public static String toHex(long a) {

	    String result = Long.toString(a, 16);

	    return result.toUpperCase();

    }

}
