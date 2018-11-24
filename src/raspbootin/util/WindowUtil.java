package raspbootin.util;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

public class WindowUtil {

	public static void setLocation(int x, int y, int w, int h, Window frame) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (x > screenSize.getWidth()) {
			if ( w > screenSize.getWidth()) {
				x = 10;
				w = (int)(screenSize.getWidth() - 10);
			} else {
				x = (int)(screenSize.getWidth() - w);
			}
		} else if ((screenSize.getWidth() - x) < 100) {
			x = (int)(screenSize.getWidth() - 100); 
		} 
		if (y > screenSize.getHeight()) {
			if (y + h > screenSize.getHeight()) {
				y = 10;
				h = (int)(screenSize.getHeight() - 10);
			} else {
				y = (int)(screenSize.getHeight() - h);
			}
		} else if ((screenSize.getHeight() - y) < 100) {
			y = (int)(screenSize.getHeight() - 100); 
		} 
		frame.setBounds(x, y, w, h);
	}
}
