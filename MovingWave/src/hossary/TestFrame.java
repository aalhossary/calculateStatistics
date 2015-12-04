package hossary;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.border.EmptyBorder;

public class TestFrame extends JFrame {
	private static final long serialVersionUID = -8119815446616625464L;

	private WavePanel wavePanel;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TestFrame frame = new TestFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public TestFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 500, 500);
		wavePanel = new WavePanel();
		wavePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		wavePanel.setLayout(new BorderLayout(0, 0));
		setContentPane(wavePanel);
		wavePanel.ticker.start();

	}

}
