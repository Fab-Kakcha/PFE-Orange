/**
 * GUI to choose either SIP or WebRTC
 */
package com.orange.olps.stageFabrice.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.orange.olps.stageFabrice.OmsServiceEx;
import com.orange.olps.stageFabrice.sip.MonServiceSip;

/**
 * @author JWPN9644
 *
 */
public class SwingControl {
	
	private JFrame mainFrame;
	private JLabel headerLabel;
	private ImageIcon imageIcon;
	private JLabel imageLabel;
	private JPanel controlPanel;

	private boolean isServiceLaunch = false;
	private static Logger logger = Logger.getLogger(OmsServiceEx.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		SwingControl swingControl = new SwingControl();
		swingControl.showButton();
	}
	
	public SwingControl(){
		
		prepareGUI();
	}
	
	private void prepareGUI(){
		
		mainFrame = new JFrame("Welcome to OMS's GUI");
		mainFrame.setSize(400,400);
		mainFrame.setLayout(new GridLayout(3,7));
		mainFrame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent windowEvent){
				if(!isServiceLaunch)
					System.exit(0);
			}
		});
		
		headerLabel = new JLabel("", JLabel.CENTER);
		//statusLabel = new JLabel("", JLabel.CENTER);
		imageIcon = new ImageIcon("C:\\Users\\JWPN9644\\Pictures\\logo.png");
		imageLabel = new JLabel(imageIcon, JLabel.CENTER);
		
		//statusLabel.setSize(350,100);
		controlPanel = new JPanel();
		controlPanel.setLayout(new FlowLayout());
		
		mainFrame.add(headerLabel);
		mainFrame.add(controlPanel);
		mainFrame.add(imageLabel);
		//mainFrame.add(statusLabel);
		//mainFrame.getContentPane().setBackground(Color.BLUE);
		mainFrame.setVisible(true);		
	}
	
	private void showButton(){
		
		headerLabel.setText("Choose either SIP or WebRTC");
		
		JButton sipButton = new JButton("SIP");
		JButton webrtcButton = new JButton("WebRTC");
		
		sipButton.setForeground(Color.BLUE);
		sipButton.setBackground(Color.LIGHT_GRAY);
		sipButton.setOpaque(true);
		
		webrtcButton.setForeground(Color.BLUE);
		webrtcButton.setBackground(Color.LIGHT_GRAY);
		webrtcButton.setOpaque(true);
		
		sipButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				//statusLabel.setText("SIP Button clicked ");
				isServiceLaunch = true;
				mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
				
				new MonServiceSip();				
			}
		});
		
		
		webrtcButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				//statusLabel.setText("WebRTC Button clicked");
				isServiceLaunch = true;
				mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
							
					try {
						 
						OmsServiceEx oms = new OmsServiceEx();
						oms.start(); 
						OmsServiceEx.dort(500);
						logger.info("OmsGateway started on port: " + oms.getPort());
					} catch (InterruptedException | IOException e1) {
						// TODO Auto-generated catch block
						System.err.println("Erreur au lancement du serveur en WebRTC");
						e1.printStackTrace();
						System.exit(0);
					}					 			
			}
		});
		
		controlPanel.add(sipButton);
		controlPanel.add(webrtcButton);
		mainFrame.setVisible(true);
	}
}
