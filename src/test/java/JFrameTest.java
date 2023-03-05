import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

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
		frame.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				System.out.println(e);
			}
		});
		frame.setVisible(true);
	}
}
