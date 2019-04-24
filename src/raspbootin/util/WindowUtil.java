package raspbootin.util;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;

public class WindowUtil {

	public static void setLocation(int x, int y, int w, int h, Window frame) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();

		if (gs.length > 0) {
			screenSize.width = 0;
			
			for (int j = 0; j < gs.length; j++) {
				GraphicsDevice gd = gs[j];
				GraphicsConfiguration[] gcc = gd.getConfigurations();
				for (GraphicsConfiguration gc : gcc) {
					System.out.println(gd + "" + gc.getBounds());
					screenSize.width += gc.getBounds().width;
				}
			}
		}

		if (x > screenSize.getWidth()) {
			if (w > screenSize.getWidth()) {
				x = 10;
				w = (int) (screenSize.getWidth() - 10);
			} else {
				x = (int) (screenSize.getWidth() - w);
			}
		} else if ((screenSize.getWidth() - x) < 100) {
			x = (int) (screenSize.getWidth() - 100);
		}
		if (y > screenSize.getHeight()) {
			if (y + h > screenSize.getHeight()) {
				y = 10;
				h = (int) (screenSize.getHeight() - 10);
			} else {
				y = (int) (screenSize.getHeight() - h);
			}
		} else if ((screenSize.getHeight() - y) < 100) {
			y = (int) (screenSize.getHeight() - 100);
		}
		frame.setBounds(x, y, w, h);
	}
}
