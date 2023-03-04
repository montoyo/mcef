import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class JFrameTest {
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				System.out.println(e);
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				System.out.println(e);
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
			
			}
		});
		frame.setVisible(true);
	}
}
