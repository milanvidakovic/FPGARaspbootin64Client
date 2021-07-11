package raspbootin;

import jssc.SerialPort;
import jssc.SerialPortException;

public class Test {
	public static void main(String[] args) throws Exception {
		SerialPort serialPort = new SerialPort("COM4");
		if (!serialPort.isOpened()) {
			// Open serial port
			serialPort.openPort();
			// Set params. Also you can set params by this string:
			// serialPort.setParams(9600, 8, 1, 0);
			serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
		} 
		serialPort.writeByte((byte) 'M');
		serialPort.writeByte((byte) 13);
		serialPort.writeByte((byte) 10);
		serialPort.writeByte((byte) 3);
		serialPort.writeByte((byte) 3);
		serialPort.writeByte((byte) 3);
		Thread.sleep(100);
		byte[] buffer = serialPort.readBytes(1);
		byte b1 = buffer[0];
		buffer = serialPort.readBytes(1);
		byte b2 = buffer[0];
		buffer = serialPort.readBytes(1);
		System.out.println("b1: " + b1 + ", b2: " + b2);
		byte b3 = buffer[0];
		buffer = serialPort.readBytes(1);
		byte b4 = buffer[0];
		System.out.println("b3: " + b3 + ", b4: " + b4);
		int size1 = calcSize(b1, b2);
		System.out.println("Received image size: " + size1);
		int size = size1;
		serialPort.writeByte((byte) (size & 255));
		Thread.sleep(100);
		size >>= 8;
		serialPort.writeByte((byte) (size & 255));
		System.out.println("Sent image size confirmation");
		
		
		short sum = 0;
		int b;
		buffer = serialPort.readBytes(size1);
		for (int i = 0; i < size1; i++) {
			b = buffer[i];
			sum += b < 0 ? 256 + b : b;
		}
		System.out.println("Calculated sum: " + sum);
		
		serialPort.writeByte((byte) (sum & 255));
		Thread.sleep(100);
		sum >>= 8;
		serialPort.writeByte((byte) (sum & 255));
		
		serialPort.closePort();
		System.out.println("END");
	}

	private static int calcSize(byte b1, byte b2) {
		int B1 = b1 < 0 ? 256 + b1 : b1;
		int B2 = b2 < 0 ? 256 + b2 : b2;
		System.out.println("b1: " + b1 + ", b2: " + b2);
		System.out.println("B1: " + B1 + ", B2: " + B2);
		return B1 + B2 * 256;
	}

	private static int calcSize(byte[] buffer) {
		byte b1 = buffer[0];
		byte b2 = buffer[1];
		int B1 = b1 < 0 ? 256 + b1 : b1;
		int B2 = b2 < 0 ? 256 + b2 : b2;
		System.out.println("b1: " + b1 + ", b2: " + b2);
		System.out.println("b3: " + buffer[2] + ", b4: " + buffer[3]);
		System.out.println("B1: " + B1 + ", B2: " + B2);
		return B1 + B2 * 256;
	}
}
