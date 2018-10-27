package raspbootin;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Consumer;

import jssc.SerialPort;
import jssc.SerialPortException;
import raspbootin.gui.MainFrame;

public class Raspbootin64Client {

	public static SerialPort connectAndSend(SerialPort serialPort, String fileName, Consumer<String> print) throws Exception {
		try {
			if (!serialPort.isOpened()) {
				// Open serial port
				serialPort.openPort();
				// Set params. Also you can set params by this string:
				// serialPort.setParams(9600, 8, 1, 0);
				serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
			} else {
				serialPort.removeEventListener();
			}
			print.accept("Connecting to the Raspbootin...");
			// Read 8+3 bytes from the serial port (name + load_ready)
			byte[] buffer;
			String s = "";
			do {
				buffer = serialPort.readBytes(1);
				if (buffer[0] != '\r' && buffer[0] != '\n' && buffer[0] != 3) {
					s += new String(buffer);
				}
			} while (buffer[0] != 3);
			
			Thread.sleep(100);

			buffer = new byte[2];
			// get two more 0x03 bytes
			buffer = serialPort.readBytes(2);
			print.accept("\nRaspbootin ready: [" + s + "]");

			Thread.sleep(100);

			if (buffer[0] == 3 && buffer[1] == 3) {
				print.accept("\nReady to send kernel file " + fileName);
				// ready to send file
				File f = new File(fileName);

				if (f.exists() && f.isFile()) {

					long size = f.length();
					size -= 1024L;
					long sizeToSend = size;

					Thread.sleep(100);
					print.accept("\nReady to send " + size + " bytes.");

					serialPort.writeByte((byte) (size & 255));
					Thread.sleep(100);
					size >>= 8;
					serialPort.writeByte((byte) (size & 255));
					Thread.sleep(100);
					size >>= 8;
					serialPort.writeByte((byte) (size & 255));
					Thread.sleep(100);
					size >>= 8;
					serialPort.writeByte((byte) (size & 255));
					Thread.sleep(100);

					// Read 2 bytes from the serial port (file size acknowledge or error)
					buffer = serialPort.readBytes(2);

					Thread.sleep(100);
					
					int returnSize = calcSize(buffer);
					print.accept("\nGot the response from Raspbootin64: " + returnSize);
					
//					if (buffer[0] == 'O' && buffer[1] == 'K') {
					if (returnSize == sizeToSend) {
						// File length OK, proceed with upload.
						FileInputStream in = new FileInputStream(f);
						buffer = new byte[1024];
						int read, total = 0;
						for (int i = 0; i < 1024; i++) {
							in.read();
						}
						short sum = 0;
						byte b;
						while ((read = in.read(buffer)) != -1) {
							for (int i = 0; i < read; i++) {
								b = buffer[i];
								serialPort.writeByte(b);
								//Thread.sleep(1);
								sum += b < 0 ? 256 + b : b;
							}
							total += read;
						}
						in.close();

						Thread.sleep(100);
						
						print.accept(
								"\nSent " + total + " bytes.\n");
						Thread.sleep(100);
						print.accept("Received checksum: " + (sum < 0 ? 65536 + sum : sum) + "\n");
						Thread.sleep(100);
						int bytesLeft = serialPort.getInputBufferBytesCount(); 
						System.out.println("Bytes left: " + bytesLeft);
						if (bytesLeft >= 2) {
							buffer = serialPort.readBytes(bytesLeft);
							for (int i = 0; i < buffer.length; i++) {
								System.out.printf("%02x ", buffer[i]);
							}
							int checksum = calcSize(buffer);
							int isum = (sum > 0) ? sum : 65536 + sum;
							if (checksum == isum) {
								print.accept("OK, received checksum is the same as the calculated checksum: " + isum);
							} else {
								print.accept("ERROR, received checksum WRONG: " + checksum + ". Calculated checksum: " + isum);
							}
						} else {
							print.accept("\nERROR, did not receive file checksum from FPGA, or wrong number of bytes received.");
							buffer = serialPort.readBytes(bytesLeft);
							for (int i = 0; i < buffer.length; i++) {
								System.out.printf("%02x ", buffer[i]);
							}
						}
					} else {
						Thread.sleep(100);
						print.accept("\nERROR, file size error reported by the FPGA: [" + returnSize + "]");
					}
				} else {
					Thread.sleep(100);
					print.accept("\nERROR, File " + fileName + " does not exist, or is a folder!");
				}
			} else {
				Thread.sleep(100);
				print.accept("\nERROR, got wrong bytes from FPGA Raspbootin: [" + new String(buffer) + "]");
			}
		} catch (SerialPortException ex) {
			System.out.println(ex);
		}
		return serialPort;
	}

	private static int calcSize(byte[] buffer) {
		byte b1 = buffer[0];
		byte b2 = buffer[1];
		int B1 = b1 < 0 ? 256 + b1 : b1;
		int B2 = b2 < 0 ? 256 + b2 : b2;
		return B1 + B2 * 256;
	}

	public static void main(String[] args) {
		try {
			if (args.length == 2) {
				Raspbootin64Client.connectAndSend(new SerialPort(args[0]), args[1], toPrint -> System.out.print(toPrint)).closePort();
			} else if (args.length == 0 || args.length > 2) {
				System.err.println("Usage: java FPGARaspbootin64Client <com_port> <file_path>");
				System.err.println("Example: java FPGARaspbootin64Client COM3 C:\\Temp\\kernel8.img");
			} else if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
				new MainFrame();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
