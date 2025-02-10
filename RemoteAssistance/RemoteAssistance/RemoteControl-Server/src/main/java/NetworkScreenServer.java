import com.sun.jna.Library;
import com.sun.jna.Native;
import org.xerial.snappy.Snappy;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Vector;


public class NetworkScreenServer extends JFrame {
	private final static int SERVER_PORT = 9999;
	private final static int SERVER_CURSOR_PORT = SERVER_PORT - 1;
	private final static int SERVER_KEYBOARD_PORT = SERVER_PORT - 2;
	private final static int SERVER_CHAT_PORT = SERVER_PORT - 3;  // Giả sử SERVER_PORT là 9999

	private DataOutputStream dataOutputStream;
	private ObjectOutputStream objectOutputStream;
	private Image cursor;
	private String myFont = "????";
	private BufferedImage screenImage;
	private Rectangle rect;
	private MainPanel mainPanel = new MainPanel();
	private ServerSocket imageSeverSocket = null;
	private ServerSocket cursorServerSocket = null;
	private ServerSocket keyboardServerSocket = null;
	private Socket imageSocket = null;
	private Socket cursorSocket = null;
	private Socket keyboardSocket = null;
	private Robot robot;
	private int screenWidth, screenHeight;
	private Boolean isRunning = false;
	private Thread imgThread;
	private static int new_Width = 1920;
	private static int new_Height = 1080;
	private JButton startBtn;
	private JButton stopBtn;
	private JButton chatBtn;
	private JTextField widthTextfield;
	private JTextField heightTextfield;
	private JRadioButton compressTrueRBtn;
	private JRadioButton compressFalseRBtn;
	private JLabel widthLabel;
	private JLabel heightLabel;
	private JLabel compressLabel;
	private URL cursorURL = getClass().getClassLoader().getResource("cursor.gif");
	private Boolean isCompress = true;
	private JFrame fff = this;
	private final int MOUSE_MOVE = 1;
	private final int MOUSE_PRESSD = 2;
	private final int MOUSE_RELEASED = 3;
	private final int MOUSE_DOWN_WHEEL = 4;
	private final int MOUSE_UP_WHEEL = 5;
	private final int KEY_PRESSED = 6;
	private final int KEY_RELEASED = 7;
	private final int KEY_CHANGE_LANGUAGE = 8;
	int count = 0, count2 = 0;
	private User32jna u32 = User32jna.INSTANCE;
	private int buffersize = 1;
	private BufferedImage[] img = new BufferedImage[buffersize];
	private Vector<byte[]> imgvec = new Vector<>();

	public NetworkScreenServer() {
		setTitle("NetworkScreenServer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);
		setContentPane(mainPanel);
		setSize(690, 160);
		setVisible(true);
		setResizable(false);

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				System.out.println("type" + e.getKeyCode() + "  " + e.getKeyChar() + "  " + e.getID() + "  "
						+ e.getModifiers() + "  " + e.getKeyLocation() + "  " + e.getExtendedKeyCode());
			}

			@Override
			public void keyPressed(KeyEvent e) {
				System.out.println("pressed" + e.getKeyCode() + "  " + e.getKeyChar() + "  " + e.getID() + "  "
						+ e.getModifiers() + "  " + e.getKeyLocation() + "  " + e.getExtendedKeyCode());
				super.keyPressed(e);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				System.out.println("released" + e.getKeyCode() + "  " + e.getKeyChar() + "  " + e.getID() + "  "
						+ e.getModifiers() + "  " + e.getKeyLocation() + "  " + e.getExtendedKeyCode());
				if (e.getKeyCode() == 0) {
					if (count >= 1) {
						count = 0;
						return;
					}
					// System.out.println(t.getLocale().toString() + " " +
					// t.getLocale().getCountry() + " " +
					// t.getLocale().getDisplayCountry());
					System.out.println("ee");
					count = 1;
					u32.keybd_event((byte) 0x15, (byte) 0, 0, 0);// ????ffDDDddSS
					u32.keybd_event((byte) 0x15, (byte) 00, (byte) 0x0002, 0);// ????
																				// ????
				}
			}
		});
		widthTextfield.requestFocus();
	}

	public interface User32jna extends Library {
		User32jna INSTANCE = (User32jna) Native.load("user32.dll", User32jna.class);

		// User32jna INSTANCE = (User32jna)
		// Native.loadLibrary("user32.dll",User32jna.class);
		public void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
	}

	/*
	 * User32jna u32 = User32jna.INSTANCE;
	 */

	class MainPanel extends JPanel implements Runnable {

		public MainPanel() {
			setLayout(null);

			startBtn = new JButton("Start");
			stopBtn = new JButton("Stop");
			chatBtn  = new JButton("Chat");
			widthTextfield = new JTextField(Integer.toString(new_Width), 5);
			heightTextfield = new JTextField(Integer.toString(new_Height), 5);
			widthLabel = new JLabel("width");
			heightLabel = new JLabel("height");
			compressLabel = new JLabel("<html>&nbsp&nbsp&nbsp<span>Image<br>Compress</span></html>");
			compressTrueRBtn = new JRadioButton("True");
			compressFalseRBtn = new JRadioButton("False");

			startBtn.setBounds(0, 0, 150, 130);
			stopBtn.setBounds(150, 0, 150, 130);
	        chatBtn.setBounds(490, 0, 150, 130); // Vị trí cho nút Chat

			widthLabel.setBounds(327, 8, 50, 15);
			widthTextfield.setBounds(300, 30, 90, 35);
			heightLabel.setBounds(325, 70, 50, 15);
			heightTextfield.setBounds(300, 90, 90, 35);
			compressLabel.setBounds(405, -10, 100, 50);
			compressTrueRBtn.setBounds(390, 30, 80, 30);
			compressFalseRBtn.setBounds(390, 90, 80, 30);

			ButtonGroup group = new ButtonGroup();
			group.add(compressTrueRBtn);	
			group.add(compressFalseRBtn);

			widthLabel.setFont(new Font(myFont, Font.PLAIN, 15));
			heightLabel.setFont(new Font(myFont, Font.PLAIN, 15));

			compressLabel.setFont(new Font(myFont, Font.PLAIN, 10));
			startBtn.setFont(new Font(myFont, Font.PLAIN, 20));
			stopBtn.setFont(new Font(myFont, Font.PLAIN, 20));
	        chatBtn.setFont(new Font(myFont, Font.PLAIN, 20));  // Font cho nút Chat
			compressTrueRBtn.setFont(new Font(myFont, Font.PLAIN, 20));
			compressFalseRBtn.setFont(new Font(myFont, Font.PLAIN, 20));

			compressTrueRBtn.setSelected(true);

			add(startBtn);
			add(stopBtn);
	        add(chatBtn);  // Thêm nút Chat vào giao diện
			add(widthLabel);
			add(widthTextfield);
			add(heightLabel);
			add(heightTextfield);
			add(compressLabel);
			add(compressTrueRBtn);
			add(compressFalseRBtn);
			stopBtn.setEnabled(false);
			chatBtn.addActionListener(new ActionListener() {
			    @Override
			    public void actionPerformed(ActionEvent e) {
			        // Tạo một JTextArea để hiển thị tin nhắn và truyền vào ChatThread
			        JTextArea chatArea = new JTextArea();
			        chatArea.setEditable(false);  // Đảm bảo người dùng không thể sửa tin nhắn trong chat

			        // Khởi tạo và bắt đầu thread ChatThread
			        NetworkScreenServer.ChatThread chatThread = new ChatThread(chatArea);
			        chatThread.start();  // Thread sẽ mở cửa sổ chat trong phương thức run() của nó
			    }
			});

			startBtn.addActionListener(new ActionListener() {
			    @Override
			    public void actionPerformed(ActionEvent e) {
			        if (isRunning)
			            return;
			        try {
			            new_Height = Integer.parseInt(heightTextfield.getText());
			            new_Width = Integer.parseInt(widthTextfield.getText());
			        } catch (Exception e1) {
			            return;
			        }
			        heightTextfield.setEditable(false);
			        widthTextfield.setEditable(false);
			        isRunning = true;
			        startBtn.setEnabled(false);
			        stopBtn.setEnabled(true);
			        if (compressTrueRBtn.isSelected()) {
			            isCompress = true;
			        } else if (compressFalseRBtn.isSelected()) {
			            isCompress = false;
			        }
			        compressTrueRBtn.setEnabled(false);
			        compressFalseRBtn.setEnabled(false);

			        imgThread = new Thread(mainPanel);
			        CursorThread cursorThread = new CursorThread();
			        KeyBoardThread keyBoardThread = new KeyBoardThread();
			        imgThread.start();
			        cursorThread.start();
			        keyBoardThread.start();
			    }
			});

			stopBtn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (!isRunning)
						return;
					heightTextfield.setEditable(true);
					widthTextfield.setEditable(true);
					isRunning = false;
					ServerSocketCloseThread closeThread = new ServerSocketCloseThread();
					closeThread.start();
					// imgThread.interrupt();
					stopBtn.setEnabled(false);
					startBtn.setEnabled(true);
					compressTrueRBtn.setEnabled(true);
					compressFalseRBtn.setEnabled(true);

				}
			});
			widthTextfield.transferFocus();
		}

		public void run() {

			try {
				robot = new Robot();
				screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
				screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
				rect = new Rectangle(0, 0, screenWidth, screenHeight);
				imageSeverSocket = new ServerSocket(SERVER_PORT);// ImageSERVER

				imageSocket = imageSeverSocket.accept();
				imageSocket.setTcpNoDelay(true);
				dataOutputStream = new DataOutputStream(imageSocket.getOutputStream());
				objectOutputStream = new ObjectOutputStream(imageSocket.getOutputStream());
				dataOutputStream.writeInt(screenWidth);
				dataOutputStream.writeInt(screenHeight);
				dataOutputStream.writeInt(new_Width);
				dataOutputStream.writeInt(new_Height);
				dataOutputStream.writeBoolean(isCompress);
				cursor = ImageIO.read(cursorURL);
			} catch (Exception e) {

			}
			ImgDoubleBufferTh th = new ImgDoubleBufferTh();
			th.start();					
			
			 
			int index = 0;
			Runtime runtime = Runtime.getRuntime();
			while (isRunning) {
				try {
					
					 					
					byte[] imageByte = imgvec.get(0);
					if(imgvec.size() == 3){
						synchronized (th) {
							th.notify();
						}						
					}
					 

					if (isCompress) {

						 
						dataOutputStream.writeInt(imageByte.length);
						dataOutputStream.write(imageByte);

						// System.out.println(imageByte.length);
						dataOutputStream.flush();
					} else {
						dataOutputStream.writeInt(imageByte.length);
						dataOutputStream.write(imageByte);
						// System.out.println(imageByte.length);

						dataOutputStream.flush();
					}
					//}
				} catch (Exception e) {

				}
				if (runtime.totalMemory() / 1024 / 1024 > 500)
					System.gc();
				if (imgvec.size() > 1) {
					/*		new Thread(){
								public void run(){						*/	
									//System.out.println(imgvec.size());
									imgvec.remove(0);
									index++;								
									if(index == 30){
										index=0;
										System.gc();
									}
						/*		}						
							}.start();*/
						}
								

				// Thread.sleep(1000);
			}

		}

	}

	class ImgDoubleBufferTh extends Thread {
		BufferedImage bufferimage;
		Robot robot = null;
		
		synchronized public void run() {			
			try {
				robot = new Robot();
			} catch (AWTException e) {
			}			
			while (true) {

				bufferimage = robot.createScreenCapture(rect);
				bufferimage = getScaledImage(bufferimage, new_Width, new_Height, BufferedImage.TYPE_3BYTE_BGR);
				byte[] imageByte = ((DataBufferByte) bufferimage.getRaster().getDataBuffer()).getData();
				try {
					imgvec.addElement(compress(imageByte));
				} catch (IOException e) {
					e.printStackTrace();
				}
				//System.out.println(imgvec.size());
				if(imgvec.size()>5)
					try {
						//System.out.println("wait");
						wait();
					} catch (InterruptedException e) {
						
					}				
			}

		}
	}
	

	public static byte[] compress(byte[] data) throws IOException {
		byte[] output = Snappy.compress(data);

		return output;
	}

	public BufferedImage getScaledImage(BufferedImage myImage, int width, int height, int type) {
		BufferedImage background = new BufferedImage(width, height, type);
		Graphics2D g = background.createGraphics();
		g.setColor(Color.WHITE);
		g.drawImage(myImage, 0, 0, width, height, null);
		g.dispose();
		return background;
	}
	class ChatThread extends Thread {
	    private static final int SERVER_CHAT_PORT = SERVER_PORT - 3;  // Giả sử SERVER_PORT là 9999
	    private ServerSocket chatServerSocket;
	    private Socket chatSocket;
	    private DataInputStream dataInputStream;
	    private DataOutputStream dataOutputStream;
	    private boolean isClientConnected = false;
	    private boolean isRunning = true; // Biến điều khiển vòng lặp
	    private JTextArea chatArea;
	    private JFrame chatFrame;

	    public ChatThread(JTextArea chatArea) {
	        this.chatArea = chatArea;
	    }

	    @Override
	    public void run() {
	        openChatWindow(); // Mở cửa sổ chat khi thread bắt đầu

	        try {
	            // Khởi tạo chatServerSocket với cổng chat
	            chatServerSocket = new ServerSocket(SERVER_CHAT_PORT);
	            System.out.println("Chat server started...");

	            // Chờ kết nối từ client
	            while (!isClientConnected) {
	                try {
	                    chatSocket = chatServerSocket.accept();  // Chờ kết nối từ client
	                    isClientConnected = true;  // Khi có kết nối, đổi trạng thái
	                    System.out.println("Client connected to chat server.");
	                } catch (IOException e) {
	                    System.out.println("Chưa có client kết nối, tiếp tục chờ...");
	                }
	            }

	            // Khi có client kết nối, bắt đầu xử lý chat
	            dataInputStream = new DataInputStream(chatSocket.getInputStream());
	            dataOutputStream = new DataOutputStream(chatSocket.getOutputStream());

	            // Đọc tin nhắn từ client và gửi lại tin nhắn tới client
	            while (isRunning) {
	                try {
	                    String message = dataInputStream.readUTF(); // Đọc tin nhắn từ client
	                    System.out.println("Received message: " + message);

	                    // Cập nhật giao diện (Swing thread-safe)
	                    SwingUtilities.invokeLater(() -> chatArea.append("Client: " + message + "\n"));

	                    // Gửi lại tin nhắn cho client
	                    dataOutputStream.writeUTF(message);
	                    dataOutputStream.flush();
	                } catch (IOException e) {
	                    System.out.println("Lỗi khi đọc tin nhắn hoặc kết nối bị ngắt.");
	                    break;  // Thoát vòng lặp nếu có lỗi
	                }
	            }
	        } catch (IOException e) {
	            System.out.println("Lỗi khi khởi tạo ServerSocket cho chat server: " + e.getMessage());
	            e.printStackTrace();
	        } finally {
	            // Đảm bảo đóng kết nối khi hoàn thành
	            try {
	                if (chatSocket != null && !chatSocket.isClosed()) {
	                    chatSocket.close();
	                }
	                if (chatServerSocket != null && !chatServerSocket.isClosed()) {
	                    chatServerSocket.close();
	                }
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    // Phương thức để mở cửa sổ chat
	    private void openChatWindow() {
	        chatFrame = new JFrame("Khung Chat");
	        chatFrame.setSize(400, 600);
	        chatFrame.setLayout(new BorderLayout());
	        chatFrame.setLocation(1100, 100);

	        // Khu vực hiển thị tin nhắn
	        chatArea = new JTextArea();
	        chatArea.setEditable(false);
	        chatArea.setLineWrap(true);
	        chatArea.setWrapStyleWord(true);
	        JScrollPane scrollPane = new JScrollPane(chatArea);

	        // Khu vực nhập tin nhắn
	        JTextField inputField = new JTextField();
	        JButton sendButton = new JButton("Gửi");

	     // Hành động khi nhấn nút gửi
	        sendButton.addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                String message = inputField.getText().trim();  // Lấy tin nhắn từ input
	                if (!message.isEmpty()) {
	                    chatArea.append("                                                                                                    " + message + "\n");  // Hiển thị tin nhắn trên giao diện
	                    inputField.setText("");  // Xóa trường nhập

	                    // Gửi tin nhắn cho client
	                    sendMessageToClient(message);
	                }
	            }

	            private void sendMessageToClient(String message) {
	                try {
	                    if (dataOutputStream != null) {
	                        // Trước khi gửi, kiểm tra lại chuỗi tin nhắn
	                        if (isValidMessage(message)) {
	                            // Gửi tin nhắn cho client
	                            dataOutputStream.writeUTF("Server: " + message);
	                            dataOutputStream.flush();  // Đảm bảo gửi hết dữ liệu

	                            // In ra tin nhắn gửi từ server
	                            System.out.println("Server sent: " + message);  // Debug message gửi từ server
	                        } else {
	                            System.out.println("Tin nhắn không hợp lệ, không gửi.");
	                        }
	                    }
	                } catch (IOException e) {
	                    e.printStackTrace();
	                    System.out.println("Error while sending message to client: " + e.getMessage());  // In lỗi nếu có sự cố khi gửi
	                }
	            }

	            // Hàm kiểm tra tính hợp lệ của tin nhắn
	            private boolean isValidMessage(String message) {
	                // Kiểm tra nếu tin nhắn không rỗng và không chứa ký tự đặc biệt không hợp lệ
	                return message != null && !message.isEmpty() && !message.contains("\0");
	            }
	        });


	        // Thêm vào giao diện khung chat
	        JPanel inputPanel = new JPanel(new BorderLayout());
	        inputPanel.add(inputField, BorderLayout.CENTER);
	        inputPanel.add(sendButton, BorderLayout.EAST);

	        chatFrame.add(scrollPane, BorderLayout.CENTER);
	        chatFrame.add(inputPanel, BorderLayout.SOUTH);

	        // Hiển thị khung chat
	        chatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        chatFrame.setVisible(true);
	    }

	    // Phương thức để dừng thread một cách an toàn
	    public void stopChat() {
	        isRunning = false; // Dừng vòng lặp trong thread
	        try {
	            if (chatSocket != null && !chatSocket.isClosed()) {
	                chatSocket.close();
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	}







	class ServerSocketCloseThread extends Thread {
		public void run() {
			if (!imageSeverSocket.isClosed() || !cursorServerSocket.isClosed() || keyboardServerSocket.isClosed()) {
				try {
					imageSeverSocket.close();
					cursorServerSocket.close();
					keyboardServerSocket.close();
				} catch (IOException e) {
					DebugMessage.printDebugMessage(e);
				}
			}
		}
	}

	class KeyBoardThread extends Thread {
		public void run() {
			try {
				keyboardServerSocket = new ServerSocket(SERVER_KEYBOARD_PORT);
				keyboardSocket = keyboardServerSocket.accept();
				DataInputStream dataInputStream = new DataInputStream(keyboardSocket.getInputStream());
				while (true) {
					int keyboardState = dataInputStream.readInt();
					if (keyboardState == KEY_PRESSED) {// KEYBOARD PRESSED
						int keyCode = dataInputStream.readInt();
						// System.out.println(keyCode + "????");
						u32.keybd_event((byte) keyCode, (byte) 0, 0, 0);// ????ffDDDddSS
						// robot.keyPress(keyCode);
					} else if (keyboardState == KEY_RELEASED) {
						int keyCode = dataInputStream.readInt();
						// System.out.println(keyCode + "????");
						u32.keybd_event((byte) keyCode, (byte) 00, (byte) 0x0002, 0);// ??
						// robot.keyRelease(keyCode);
					}
					yield();
				}
			} catch (Exception e) {

			}
		}
	}

	class CursorThread extends Thread {
		public void run() {
			try {
				cursorServerSocket = new ServerSocket(SERVER_CURSOR_PORT);// cursorSERVER
				cursorSocket = cursorServerSocket.accept();
				DataInputStream dataInputStream = new DataInputStream(cursorSocket.getInputStream());
				int mouseX = 0;
				int mouseY = 0;
				while (isRunning) {
					int mouseState = dataInputStream.readInt();// mouse,Keyboard
																// state
					if (mouseState == MOUSE_MOVE) {// move
						mouseX = dataInputStream.readInt();
						mouseY = dataInputStream.readInt();

						robot.mouseMove(mouseX, mouseY);
					} else if (mouseState == MOUSE_PRESSD) { // pressed
						int mouseButton = dataInputStream.readInt();
						robot.mouseMove(mouseX, mouseY);
						if (mouseButton == 1) {
							robot.mousePress(MouseEvent.BUTTON1_MASK);
						} else if (mouseButton == 2) {
							robot.mousePress(MouseEvent.BUTTON2_MASK);
						} else if (mouseButton == 3) {
							robot.mousePress(MouseEvent.BUTTON3_MASK);
						}
					} else if (mouseState == MOUSE_RELEASED) {// released
						int mouseButton = dataInputStream.readInt();
						robot.mouseMove(mouseX, mouseY);
						if (mouseButton == 1) {
							robot.mouseRelease(MouseEvent.BUTTON1_MASK);
						} else if (mouseButton == 2) {
							robot.mouseRelease(MouseEvent.BUTTON2_MASK);
						} else if (mouseButton == 3) {
							robot.mouseRelease(MouseEvent.BUTTON3_MASK);
						}
					} else if (mouseState == MOUSE_DOWN_WHEEL) {// MOUSE DOWN
																// WHEEL
						robot.mouseWheel(-3);
					} else if (mouseState == MOUSE_UP_WHEEL) {// MOUSE UP WHEEL
						robot.mouseWheel(3);
					}
					yield();
				}

			} catch (Exception e) {

			}

		}
	}

}
