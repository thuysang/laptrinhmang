 import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class NetworkScreenClient extends JFrame {
	private ControlPanel controlPanel = new ControlPanel();
	private String myFont = "맑은고딕";
	private URL exitURL;//= getClass().getClassLoader().getResource("exit.png");
	private ImageIcon exitIcon;//= new ImageIcon(exitURL);
	private URL minimizeURL;// = getClass().getClassLoader().getResource("minimize.png");
	private ImageIcon minimizeIcon;// = new ImageIcon(minimizeURL);
	private JMenuBar jbar;
	private final int FRAME_WIDTH = 500;
	private final int FRAME_HEIGHT = 110;
	private Socket socket = new Socket();
	private Socket cursorsocket = new Socket();
	private Socket keyboardsocket = new Socket();
	private Socket chatSocket = new Socket();
	private JFrame jFrame = this;
	private JLabel networkLabel;
	private final static int SERVER_PORT = 9999;
	private final static int SERVER_CURSOR_PORT = SERVER_PORT-1;
	private final static int SERVER_KEYBOARD_PORT = SERVER_PORT-2;
	private final static int CLIENT_CHAT_PORT = SERVER_PORT - 3;
	ScreenPanel screenPanel;
	public NetworkScreenClient() {
		exitURL = getClass().getClassLoader().getResource("exit.png");
		minimizeURL = getClass().getClassLoader().getResource("minimize.png");


		exitIcon = new ImageIcon(exitURL);
		minimizeIcon = new ImageIcon(minimizeURL);


		setTitle("Giao diện client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		setLayout(null);
	//	createJMenu();
		setContentPane(controlPanel);
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		//setUndecorated(true);
		setVisible(true);	
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				setLocation(e.getXOnScreen(), e.getYOnScreen());
			}
		});	
		setLocation(400, 400);
		
	}
	
	public void createJMenu(){
		jbar = new JMenuBar();
		jbar.setPreferredSize(new Dimension(FRAME_WIDTH, 40));
		//jbar.setBackground(Color.yellow);
		jbar.setBorderPainted(false);
		jbar.setLayout(null);
		
		JLabel exitlabel = new JLabel(exitIcon);
		
		JLabel minilabel = new JLabel(minimizeIcon);
		jbar.add(exitlabel);
		jbar.add(minilabel);
		exitlabel.setBounds(FRAME_WIDTH-50, 10, 35, 35);
		minilabel.setBounds(FRAME_WIDTH-95, 10, 35, 35);
		
		exitlabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton()==1){
					System.exit(1);
				}
			}
		});
		minilabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton()==1){
					jFrame.setState(JFrame.ICONIFIED);
				}					
			}
		});
		setJMenuBar(jbar);
	}
	class ControlPanel extends JPanel{
		JTextField addressField = new JTextField(10);
		JButton connectBtn = new JButton("Connect");
		JButton exitBtn = new JButton("Exit");
		public ControlPanel() {
			setLayout(null);
			
			addressField.setBounds(0, 0, 200, FRAME_HEIGHT-50);
			connectBtn.setBounds(200, 0, 150, FRAME_HEIGHT-50);
			exitBtn.setBounds(350, 0, 150, FRAME_HEIGHT-50);
			
			addressField.setFont(new Font(myFont, Font.PLAIN, 20));
			connectBtn.setFont(new Font(myFont, Font.PLAIN, 20));
			exitBtn.setFont(new Font(myFont, Font.PLAIN, 20));
			addressField.setForeground(Color.LIGHT_GRAY);
			
			addressField.setText("123.123.123.123");
			addressField.setCaretPosition(0);
			addressField.setMargin(new Insets(1, 15, 1, 15));
			addressField.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					if(addressField.getText().equals("123.123.123.123") && addressField.getForeground() == Color.LIGHT_GRAY){
						addressField.setText("");
						addressField.setForeground(Color.BLACK);
					}
					else if(addressField.getText().equals("Connect Fail")){
						addressField.setText("");
						addressField.setForeground(Color.BLACK);
					}
				}
			});
			addressField.addKeyListener(new KeyAdapter() {			
				@Override
				public void keyReleased(KeyEvent e) {
					if(addressField.getText().equals("")){
						addressField.setForeground(Color.LIGHT_GRAY);
						addressField.setText("123.123.123.123");
						addressField.setCaretPosition(0);
					}					
				}
				
				@Override
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode() == KeyEvent.VK_ENTER){
						connectBtn.doClick();
					}
					if(addressField.getText().equals("123.123.123.123") && addressField.getForeground() == Color.LIGHT_GRAY){
						addressField.setText("");
						addressField.setForeground(Color.BLACK);
					}
					
				}
			});
			
			add(addressField);
			add(connectBtn);
			add(exitBtn);
			exitBtn.setEnabled(false);
			connectBtn.addActionListener(new ActionListener() {
			    @Override
			    public void actionPerformed(ActionEvent e) {
			        InetSocketAddress inetAddress;
			        InetSocketAddress inetCursorAddress;
			        InetSocketAddress inetKeyboardAddress;
			        InetSocketAddress inetChatAddress;  // Cổng chat mới

			        // Kiểm tra nếu người dùng chưa nhập địa chỉ IP
			        if(addressField.getText().equals("123.123.123.123") && addressField.getForeground() == Color.LIGHT_GRAY){
			            inetAddress = new InetSocketAddress("localhost", SERVER_PORT);
			            inetCursorAddress = new InetSocketAddress("localhost", SERVER_CURSOR_PORT);
			            inetKeyboardAddress = new InetSocketAddress("localhost", SERVER_KEYBOARD_PORT);
			            inetChatAddress = new InetSocketAddress("localhost", CLIENT_CHAT_PORT); // Cổng chat
			        }
			        else{
			            inetAddress = new InetSocketAddress(addressField.getText(), SERVER_PORT);
			            inetCursorAddress = new InetSocketAddress(addressField.getText(), SERVER_CURSOR_PORT);
			            inetKeyboardAddress = new InetSocketAddress(addressField.getText(), SERVER_KEYBOARD_PORT);
			            inetChatAddress = new InetSocketAddress(addressField.getText(), CLIENT_CHAT_PORT); // Cổng chat
			        }

			        try {
			            // Kết nối với các socket
			            socket.connect(inetAddress, 5000);
			            cursorsocket.connect(inetCursorAddress, 5000);
			            keyboardsocket.connect(inetKeyboardAddress, 5000);
			            chatSocket.connect(inetChatAddress, 5000);  // Kết nối với cổng chat

			        } catch (IOException e1) {
			            DebugMessage.printDebugMessage(e1);
			            addressField.setText("Connect Fail");
			            socket = new Socket();
			            cursorsocket = new Socket();
			            keyboardsocket = new Socket();
			            chatSocket = new Socket(); // Khởi tạo lại socket chat
			        }

			        if(socket.isConnected()){
			            addressField.setText("Connect Success!");
			            System.out.println("Connect Success!");

			            try {
			                Thread.sleep(1500);
			                screenPanel = new ScreenPanel(jFrame, socket, cursorsocket, keyboardsocket, chatSocket);  // Truyền socket chat vào
			                setJMenuBar(null);
			                jFrame.setContentPane(screenPanel);
			                screenPanel.requestFocus();
			                jFrame.revalidate();
			                screenPanel.requestFocus();

			            } catch (InterruptedException e1) {
			                DebugMessage.printDebugMessage(e1);
			            }
			        }
			    }
			});

			exitBtn.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					System.exit(1);
				}
			});
		}		
	}
	public static void main(String[] args) {
		new NetworkScreenClient();
	}
}