package raspbootin.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Rest {
	public static boolean started = false;
	public static String path;

	public static void init(String path) {
		Rest.path = path;
//			Service http_status_vrata = Service.ignite();
//			http_status_vrata.port(80);
		// http_status_vrata.staticFiles.externalLocation(new
		// File("./static").getCanonicalPath());
		// initPaths(http_status_vrata);

		started = true;
		new Thread() {
			@Override
			public void run() {
				try {
					@SuppressWarnings("resource")
					ServerSocket ss = new ServerSocket(80);
					while (true) {
						Socket s = ss.accept();
						System.out.println("Client connected.");
						new RestThread(s);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}.start();
	}

}

class RestThread extends Thread {
	public Socket s;

	public RestThread(Socket s) {
		this.s = s;
		start();
	}

	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);

			String req = in.readLine();
			if (req.startsWith("GET /dir")) {
				File currFile = new File(Rest.path);
				File dir = currFile.getParentFile().getParentFile();
				System.out.println(dir.getCanonicalPath());
				File[] files = dir.listFiles();
				StringBuilder sb = new StringBuilder();
				for (File f : files) {
					if (f.isDirectory()) {
						//sb.append("<" + f.getName() + ">");
						//sb.append("\n");
					}
				}
				for (File f : files) {
					if (f.isFile()) {
						sb.append(f.getName());
						sb.append("\n");
					}
				}
				String str = sb.toString();
				int size = str.length();
				System.out.println("size: " + size);
				out.print(str);
			} else if (req.startsWith("GET /load:")) {
				String file = req.substring(req.indexOf(":"));
				file = file.substring(1, file.lastIndexOf(" ")).trim();
				System.out.println("Load file: " + file);
				File currFile = new File(Rest.path);
				File dir = currFile.getParentFile().getParentFile();
				File f = new File(dir, file);
				if (f.exists()) {
					FileInputStream fin = new FileInputStream(f);
					long size = f.length();
					byte[] buffer = new byte[(int) (f.length() + 4)];
					buffer[0] = (byte) (size & 255);
					size >>= 8;
					buffer[1] = (byte) (size & 255);
					size >>= 8;
					buffer[2] = (byte) (size & 255);
					size >>= 8;
					buffer[3] = (byte) (size & 255);
					int read;
					read = fin.read(buffer, 4, (int) f.length());
					s.getOutputStream().write(buffer, 0, read + 4);
					// BLAST!
					for (int i = 0; i < 550; i++)
						s.getOutputStream().write(0);
					fin.close();
				} else {
					System.out.println("File does not exist!");
					byte[] buff = new byte[30];
					s.getOutputStream().write(buff);
				}
				s.getOutputStream().flush();
				out.flush();
			}
			out.close();
			in.close();
			s.close();
		} catch (

		Exception ex) {
			ex.printStackTrace();
		}
	}
}
