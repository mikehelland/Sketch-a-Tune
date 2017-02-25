package com.monadpad.sketchatune2.record;

import android.content.Context;
import android.util.Log;
import com.monadpad.sketchatune2.record.dsp.UGen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: MGH
 * Date: 10/13/11
 * Time: 6:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class PcmWriter extends Thread{

	// our private variables

	private String myPath;
	private long myChunkSize;
	private long mySubChunk1Size = 16;
	private int myFormat = 1;
	private int myChannels = 2;
	private long mySampleRate = (long) UGen.SAMPLE_RATE ;
	private long myByteRate;
	private int myBlockAlign;
	private int myBitsPerSample = 16;
	private long myDataSize = 0;

	// I made this public so that you can toss whatever you want in here
	// maybe a recorded buffer, maybe just whatever you want
	public byte[] myData;

    private boolean finished = false;
    private boolean tryToFinish = false;
    private boolean runningFaster = false;


    private int samples = 0;
    private Context context;
    private String headername = "temp.wav";

	// get/set for the Path property
	public String getPath()
	{
		return myPath;
	}
	public void setPath(String newPath)
	{
		myPath = newPath;
	}

	// empty constructor

	// constructor takes a wav path
	public PcmWriter(Context ctx, String tmpPath){

        context = ctx;
        myPath = tmpPath;

    }

    @Override
    public void run(){

        //Process.setThreadPriority(-5);
        DataOutputStream outFile = null;

        try {

              outFile = new DataOutputStream(context.openFileOutput(myPath, Context.MODE_WORLD_READABLE));
            } catch (FileNotFoundException e){
                            System.out.println(e.getMessage());
            }


        Log.d("MGH", "write thread starting now");

        while (!finished){
            if (bdlist.size() > 0){
                short[][] tbd = bdlist.get(0);

                for (int is = 0 ; is < tbd[0].length; is++){

                    for (int ic = 0; ic < 2; ic++){

                        try{

                            outFile.write(shortToByteArray(tbd[ic][is]));

                        }   catch(Exception e){
                               // Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG);
                                //System.out.println(finished);
                                //System.out.println(e.getMessage());
                            }

                    }
                    samples++;
                }
                bdlist.remove(0);
            }

		    if (tryToFinish ){
                if (bdlist.size()  ==0)
                    finished = true;
                else if (!runningFaster) {
                    //Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                    //Process.setThreadPriority(-20);
                    //setPriority(Thread.MAX_PRIORITY);
                    runningFaster = true;
                }

            }

        }

        if (outFile != null){
            try{
            outFile.close();
            save();

          //  FilePoster fp2 = new FilePoster(context, headername);

            }
            catch(Exception e)
            {
                System.out.println(e.getMessage());
            }
            outFile = null;
        }

    }



	// write out the wav file
	public boolean save()
	{
        Log.d("MGH", "save starting now");
		try
		{

            int dataSize = samples * myChannels * myBitsPerSample/8 ;
            DataOutputStream outFile = new DataOutputStream(context.openFileOutput(headername, Context.MODE_WORLD_READABLE));

			// write the wav file per the wav file format
			outFile.writeBytes("RIFF");					// 00 - RIFF
	//		outFile.write(intToByteArray((int)myChunkSize), 0, 4);		// 04 - how big is the rest of this file?

            outFile.write(intToByteArray(36 + (dataSize)), 0, 4);		// 04 - how big is the rest of this file?

			outFile.writeBytes("WAVE");					// 08 - WAVE
			outFile.writeBytes("fmt ");					// 12 - fmt

			outFile.write(intToByteArray((int)mySubChunk1Size), 0, 4);	// 16 - size of this chunk
			outFile.write(shortToByteArray((short)myFormat), 0, 2);		// 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
			outFile.write(shortToByteArray((short)myChannels), 0, 2);	// 22 - mono or stereo? 1 or 2?  (or 5 or ???)
			outFile.write(intToByteArray((int) mySampleRate), 0, 4);		// 24 - samples per second (numbers per second)

			//outFile.write(intToByteArray((int)myByteRate), 0, 4);		// 28 - bytes per second
            outFile.write(intToByteArray((int)mySampleRate * myChannels * myBitsPerSample/8), 0, 4);		// 28 - bytes per second

			outFile.write(shortToByteArray((short) (myChannels * myBitsPerSample/8)), 0, 2);	// 32 - # of bytes in one sample, for all channels

			outFile.write(shortToByteArray((short)myBitsPerSample), 0, 2);	// 34 - how many bits in a sample(number)?  usually 16 or 24
			outFile.writeBytes("data");					// 36 - data


            outFile.write(intToByteArray(dataSize), 0, 4);		// 40 - how big is this data chunk

            DataInputStream inFile = new DataInputStream(context.openFileInput(myPath));

            //byte[] pcmData = new byte[dataSize];
            //inFile.read(pcmData);
			//outFile.write(pcmData);						// 44 - the actual data itself - just a long string of numbers

            Log.d("MGH", "save writing to final file starting now");


            byte[] moveBuffer = new byte[64 * 1024];

            while (inFile.read(moveBuffer) > -1) {
                outFile.write(moveBuffer);
            }

            //for (int ibyte = 0; ibyte < dataSize; ibyte++){
            //    outFile.write(inFile.read());
            //}

            Log.d("MGH", "save writing to final file ending now");
            inFile.close();
            inFile = null;
            outFile.close();
            outFile = null;
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			return false;
		}


		return true;
	}

    //private ArrayList<short[]> bufferData = new ArrayList<short[]>();
    private short[][] bd ;
    private int bdrow = 0;
    private ArrayList<short[][]> bdlist = new ArrayList<short[][]>();

    	// write out the wav file
	public void write(short[] audioData)
	{
        if (bdrow == 0){
            bd = new short[2][];
            bd[0] = audioData;
            bdrow++;
        }  else {
            bd[1] = audioData;
            bdrow = 0;
        }

        //bufferData.add(audioData);
    }

    public void flush(){
        bdlist.add(bd);
	}

    public void finish(){
        tryToFinish = true;
    }

    public int getSamples(){
        return samples;
    }

	// return a printable summary of the wav file
	public String getSummary()
	{
		//String newline = System.getProperty("line.separator");
		String newline = "<br>";
		String summary = "<html>Format: " + myFormat + newline + "Channels: " + myChannels + newline + "SampleRate: " + mySampleRate + newline + "ByteRate: " + myByteRate + newline + "BlockAlign: " + myBlockAlign + newline + "BitsPerSample: " + myBitsPerSample + newline + "DataSize: " + myDataSize + "</html>";
		return summary;
	}


// ===========================
// CONVERT BYTES TO JAVA TYPES
// ===========================

	// these two routines convert a byte array to a unsigned short
	public static int byteArrayToInt(byte[] b)
	{
		int start = 0;
		int low = b[start] & 0xff;
		int high = b[start+1] & 0xff;
		return (int)( high << 8 | low );
	}


	// these two routines convert a byte array to an unsigned integer
	public static long byteArrayToLong(byte[] b)
	{
		int start = 0;
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++)
		{
			tmp[cnt] = b[i];
			cnt++;
		}
		long accum = 0;
		i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 )
		{
			accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}


// ===========================
// CONVERT JAVA TYPES TO BYTES
// ===========================
	// returns a byte array of length 4
	private static byte[] intToByteArray(int i)
	{
		byte[] b = new byte[4];
		b[0] = (byte) (i & 0x00FF);
		b[1] = (byte) ((i >> 8) & 0x000000FF);
		b[2] = (byte) ((i >> 16) & 0x000000FF);
		b[3] = (byte) ((i >> 24) & 0x000000FF);
		return b;
	}

	// convert a short to a byte array
	public static byte[] shortToByteArray(short data)
	{
		return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
	}


    	// read a wav file into this class
	public boolean read()
	{
		DataInputStream inFile = null;
		myData = null;
		byte[] tmpLong = new byte[4];
		byte[] tmpInt = new byte[2];

		try
		{
			inFile = new DataInputStream(new FileInputStream(myPath));

			//System.out.println("Reading wav file...\n"); // for debugging only

			String chunkID = "" + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte();

			inFile.read(tmpLong); // read the ChunkSize
			myChunkSize = byteArrayToLong(tmpLong);

			String format = "" + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte();

			// print what we've read so far
			//System.out.println("chunkID:" + chunkID + " chunk1Size:" + myChunkSize + " format:" + format); // for debugging only



			String subChunk1ID = "" + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte();

			inFile.read(tmpLong); // read the SubChunk1Size
			mySubChunk1Size = byteArrayToLong(tmpLong);

			inFile.read(tmpInt); // read the audio format.  This should be 1 for PCM
			myFormat = byteArrayToInt(tmpInt);

			inFile.read(tmpInt); // read the # of channels (1 or 2)
			myChannels = byteArrayToInt(tmpInt);

			inFile.read(tmpLong); // read the samplerate
			mySampleRate = byteArrayToLong(tmpLong);

			inFile.read(tmpLong); // read the byterate
			myByteRate = byteArrayToLong(tmpLong);

			inFile.read(tmpInt); // read the blockalign
			myBlockAlign = byteArrayToInt(tmpInt);

			inFile.read(tmpInt); // read the bitspersample
			myBitsPerSample = byteArrayToInt(tmpInt);

			// print what we've read so far
			//System.out.println("SubChunk1ID:" + subChunk1ID + " SubChunk1Size:" + mySubChunk1Size + " AudioFormat:" + myFormat + " Channels:" + myChannels + " SampleRate:" + mySampleRate);


			// read the data chunk header - reading this IS necessary, because not all wav files will have the data chunk here - for now, we're just assuming that the data chunk is here
			String dataChunkID = "" + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte();

			inFile.read(tmpLong); // read the size of the data
			myDataSize = byteArrayToLong(tmpLong);


			// read the data chunk
			byte[] pcmdata = new byte[(int)myDataSize];
			inFile.read(myData);

			// close the input stream
			inFile.close();
		}
		catch(Exception e)
		{
			return false;
		}

		return true; // this should probably be something more descriptive
	}


}

/*

    private class FilePoster {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;

        String pathToOurFile = "/data/file_to_send.mp3";
        String urlServer = "http://simplywhimsicalgifts.com/mp/handle_upload.php";
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary =  "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1*1024*1024;

        private Context context;

        public FilePoster(Context ctx, String filename){

            pathToOurFile = filename;

            try{
                //FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile) );
                FileInputStream fileInputStream = ctx.openFileInput( pathToOurFile );

                URL url = new URL(urlServer);
                connection = (HttpURLConnection) url.openConnection();

                // Allow Inputs & Outputs
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                // Enable POST method
                connection.setRequestMethod("POST");

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

                outputStream = new DataOutputStream( connection.getOutputStream() );
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile +"\"" + lineEnd);
                outputStream.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0)
                {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                int serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                fileInputStream.close();
                outputStream.flush();
                outputStream.close();
            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
            }
        }
    }

*/