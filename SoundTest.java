import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class SoundTest extends JFrame implements ActionListener {

	AudioClip ac = null;
//ボタンのアクッションの設定
	public void actionPerformed (ActionEvent e) {
		String cmd = e.getActionCommand ();
		if (cmd.equals("push")) {
			ac.play(); //Soundをならす
		}
 	}

	SoundTest () {
		ac = java.applet.Applet.newAudioClip(SoundTest.class.getResource("dog2.wav"));  //音を取り込む
		JButton button = new JButton ("Push");
		button.addActionListener(this);
		button.setActionCommand("push");
		button.setBounds(40, 40, 80, 40);
		JPanel pane = new JPanel ();
		pane.setLayout(null);
		pane.add(button);
		getContentPane().add(pane, BorderLayout.CENTER);
	}

	public static void main(String[] args) {
		SoundTest frame = new SoundTest();
		frame.setTitle ("Sound");
		frame.setBounds(10, 10, 180, 170);
		frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
		frame.setVisible (true);
	}
}
