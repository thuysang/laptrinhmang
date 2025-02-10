import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;
import org.xerial.snappy.Snappy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.*;
import java.net.Socket;
import java.util.Vector;

class ScreenPanel extends JPanel implements Runnable {
	private Socket socket;
	private Socket cursorSocket;
	private Socket keyboardSocket;
	private Socket chatSocket;
	private BufferedImage screenImage;
	private JFrame frame;
	private JLabel FPSlabel;
	private int FPScount = 0;
	private BufferedImage image;
	private DataOutputStream mouseOutputStream;
	private DataOutputStream keyboardOutputStream;
	private DataInputStream dataInputStream;
	private ObjectInputStream objectInputStream;
	private BufferedWriter bufferedWriter;
	private int image_Width = 1280;
	private int image_Height = 720;
	private byte imageByte2[] = new byte[6220800];
	private int mouseX = 0, mouseY = 0;
	private int mouseClickCount = 0;
	private int mouseButton = 0;
	private int mousePosition = 0; // 1 == move 2 == click
	private int screen_Width = 1920;
	private int screen_Height = 1080;
	private Boolean isCompress = true;
	private final int MOUSE_MOVE = 1;
	private final int MOUSE_PRESSD = 2;
	private final int MOUSE_RELEASED = 3;
	private final int MOUSE_DOWN_WHEEL = 4;
	private final int MOUSE_UP_WHEEL = 5;
	private final int KEY_PRESSED = 6;
	private final int KEY_RELEASED = 7;
	private final int KEY_CHANGE_LANGUAGE = 8;
	private int count = 0;
	private Vector<byte[]> imgVec = new Vector<>();
	ScreenPanel ppp = this;
	User32 lib = User32.INSTANCE;
	User32jna u32 = User32jna.INSTANCE;
	HHOOK hhk = null;
	HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
	Thread thread;
	Object lock = new Object();
    private JPanel chatPanel = null;
    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private JButton sendButton = new JButton("Send");

    private DataInputStream chatInputStream;
    private DataOutputStream chatOutputStream;

	public interface User32jna extends Library {
		User32jna INSTANCE = (User32jna) Native.loadLibrary("user32.dll", User32jna.class);

		// User32jna INSTANCE = (User32jna)
		// Native.loadLibrary("user32.dll",User32jna.class);
		public void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
	}

	public ScreenPanel(Container container, Socket socket, Socket cursorSocket, Socket keyboardSocket,  Socket chatSocket) {
	    setLayout(null);
	    this.socket = socket;
	    this.cursorSocket = cursorSocket;
	    this.keyboardSocket = keyboardSocket;
	    this.chatSocket = chatSocket;
	    this.frame = (JFrame) container;
	    FPSlabel = new JLabel("FPS : " + Integer.toString(FPScount));
	    FPSlabel.setFont(new Font("맑은고딕", Font.BOLD, 20));
	    FPSlabel.setBounds(10, 10, 100, 50);
	    add(FPSlabel);

	    System.out.println(socket.getInetAddress());
	    
	    // Tạo một nút mới để thực hiện một hành động nào đó
	    JButton chatButton = new JButton("Chat");
	    chatButton.setBounds(10, 60, 120, 30);  // Đặt vị trí và kích thước của nút
	    chatButton.setFocusable(false);
	    chatButton.setFont(new Font("맑은고딕", Font.PLAIN, 14));
	    
	    // Thêm ActionListener cho nút
	    chatButton.addActionListener(new ActionListener() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            showChatWindow();
	        }

	        private void showChatWindow() {
	            if (chatPanel == null) {
	                chatPanel = new JPanel();
	                chatPanel.setLayout(new BorderLayout());

	                // Khởi tạo chatArea và các component
	                JTextArea chatArea = new JTextArea();
	                chatArea.setEditable(false);
	                chatArea.setLineWrap(true);
	                JScrollPane scrollPane = new JScrollPane(chatArea);
	                chatPanel.add(scrollPane, BorderLayout.CENTER);

	                JTextField inputField = new JTextField();
	                JButton sendButton = new JButton("Send");

	                // Thêm khung nhập tin nhắn và nút Gửi
	                JPanel inputPanel = new JPanel();
	                inputPanel.setLayout(new BorderLayout());
	                inputPanel.add(inputField, BorderLayout.CENTER);
	                inputPanel.add(sendButton, BorderLayout.EAST);
	                chatPanel.add(inputPanel, BorderLayout.SOUTH);

	                // Gửi tin nhắn khi nhấn nút Send
	                sendButton.addActionListener(new ActionListener() {
	                    @Override
	                    public void actionPerformed(ActionEvent e) {
	                        sendMessage(inputField, chatArea); // Gọi hàm gửi tin nhắn
	                    }

	                    private void sendMessage(JTextField inputField, JTextArea chatArea) {
	                        String message = inputField.getText().trim();
	                        if (!message.isEmpty()) {
	                            try {
	                                chatOutputStream.writeUTF(message);  // Gửi tin nhắn tới server
	                                chatOutputStream.flush();
	                                
	                                chatArea.append("                                                                                                 " + message + "\n");  // Hiển thị tin nhắn của người dùng
	                                inputField.setText("");  // Xóa trường nhập
	                            } catch (IOException e) {
	                                e.printStackTrace();
	                            }
	                        }
	                    }

	                });

	                // Cài đặt cửa sổ chat
	                JFrame chatFrame = new JFrame("Chat với Server");
	                chatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	                chatFrame.setSize(400, 600);
	                chatFrame.setLocationRelativeTo(null);
	                chatFrame.add(chatPanel);
	                chatFrame.setVisible(true);

	                // Tạo thread nhận tin nhắn từ server
	                new Thread(new Runnable() {
	                    @Override
	                    public void run() {
	                        receiveMessages(chatArea);  // Bắt đầu lắng nghe tin nhắn từ server
	                    }

	                    private void receiveMessages(JTextArea chatArea) {
	                        DataInputStream chatInputStream = null;
	                        try {
	                            chatInputStream = new DataInputStream(chatSocket.getInputStream());

	                            while (!chatSocket.isClosed()) {
	                                try {
	                                    if (chatInputStream.available() > 0) {  // Kiểm tra có dữ liệu chưa
	                                        String message = chatInputStream.readUTF();

	                                        if (message != null && !message.isEmpty()) {
	                                            System.out.println("Received from server: " + message);

	                                            // Chỉ hiển thị tin nhắn bắt đầu với "Server:"
	                                            if (message.startsWith("Server:")) {  // Chỉ hiển thị tin nhắn từ server
	                                                SwingUtilities.invokeLater(new Runnable() {
	                                                    @Override
	                                                    public void run() {
	                                                        chatArea.append(message + "\n");  // Hiển thị tin nhắn từ server
	                                                    }
	                                                });
	                                            }
	                                        }
	                                    }
	                                } catch (java.io.UTFDataFormatException e) {
	                                    System.out.println("Lỗi định dạng dữ liệu khi nhận tin nhắn từ server.");
	                                    break;
	                                } catch (IOException e) {
	                                    e.printStackTrace();
	                                    break;
	                                }
	                            }
	                        } catch (IOException e) {
	                            System.out.println("Lỗi khi tạo kết nối để nhận tin nhắn.");
	                            e.printStackTrace();
	                        } finally {
	                            try {
	                                if (chatInputStream != null) {
	                                    chatInputStream.close();
	                                    System.out.println("chatInputStream closed.");
	                                }
	                            } catch (IOException e) {
	                                e.printStackTrace();
	                            }
	                        }
	                    }



	                }).start();  // Bắt đầu thread nhận tin nhắn
	            }
	        }
	    });




	    add(chatButton); // Thêm nút vào panel
 // Thêm nút vào panel

	    try {
	        setLayout(null);
	        socket.setTcpNoDelay(true);
	        mouseOutputStream = new DataOutputStream(cursorSocket.getOutputStream());
	        keyboardOutputStream = new DataOutputStream(keyboardSocket.getOutputStream());
	        chatInputStream = new DataInputStream(chatSocket.getInputStream());
            chatOutputStream = new DataOutputStream(chatSocket.getOutputStream());
	        dataInputStream = new DataInputStream(socket.getInputStream());
	        objectInputStream = new ObjectInputStream(socket.getInputStream());
	    } catch (IOException e) {
	        DebugMessage.printDebugMessage(e);
	    }
	    
	    thread = new Thread(this);
	    thread.start();
	    KeyboardThread keyboardThread = new KeyboardThread();
	    keyboardThread.start();
	    FPSCheckThread fpsCheckThread = new FPSCheckThread();
	    fpsCheckThread.start();
	    showThread ss = new showThread();
	    ss.start();
	    
	    

	    addMouseWheelListener(new MouseWheelListener() {
	        @Override
	        public void mouseWheelMoved(MouseWheelEvent e) {
	            try {
	                int n = e.getWheelRotation();
	                if (n < 0) {
	                    mouseOutputStream.writeInt(MOUSE_DOWN_WHEEL);
	                } else {
	                    mouseOutputStream.writeInt(MOUSE_UP_WHEEL);
	                }
	            } catch (IOException e1) {
	                DebugMessage.printDebugMessage(e1);
	            }
	        }
	    });

	    addMouseMotionListener(new MouseMotionListener() {
	        @Override
	        public void mouseMoved(MouseEvent e) {
	            mouseX = e.getX() * screen_Width / getWidth();
	            mouseY = e.getY() * screen_Height / getHeight();

	            try {
	                mouseOutputStream.writeInt(MOUSE_MOVE);
	                mouseOutputStream.writeInt(mouseX);
	                mouseOutputStream.writeInt(mouseY);
	            } catch (IOException e1) {
	                DebugMessage.printDebugMessage(e1);
	            }
	        }

	        @Override
	        public void mouseDragged(MouseEvent e) {
	            mouseX = e.getX() * screen_Width / getWidth();
	            mouseY = e.getY() * screen_Height / getHeight();
	            try {
	                mouseOutputStream.writeInt(MOUSE_MOVE);
	                mouseOutputStream.writeInt(mouseX);
	                mouseOutputStream.writeInt(mouseY);
	            } catch (IOException e1) {
	                DebugMessage.printDebugMessage(e1);
	            }
	        }
	    });

	    addMouseListener(new MouseAdapter() {
	        @Override
	        public void mousePressed(MouseEvent e) {
	            mouseButton = e.getButton();
	            try {
	                mouseOutputStream.writeInt(MOUSE_PRESSD);
	                mouseOutputStream.writeInt(mouseButton);
	            } catch (IOException e1) {
	                DebugMessage.printDebugMessage(e1);
	            }
	        }

	        @Override
	        public void mouseReleased(MouseEvent e) {
	            mouseButton = e.getButton();
	            try {
	                mouseOutputStream.writeInt(MOUSE_RELEASED);
	                mouseOutputStream.writeInt(mouseButton);
	            } catch (IOException e1) {
	                DebugMessage.printDebugMessage(e1);
	            }
	        }
	    });

	    requestFocus();
	}

	public void run() {
		try {
			screen_Width = dataInputStream.readInt();
			screen_Height = dataInputStream.readInt();
			image_Width = dataInputStream.readInt();
			image_Height = dataInputStream.readInt();
			isCompress = dataInputStream.readBoolean();
		} catch (IOException e1) {
			DebugMessage.printDebugMessage(e1);
		}

		while (true) {

			try {// 172.30.1.54
				if (dataInputStream.available() > 0) {
					int length = 0;
					if (isCompress) {

						length = dataInputStream.readInt();

						byte imageByte[] = new byte[length];

						dataInputStream.readFully(imageByte, 0, length);

						imgVec.addElement(imageByte);
						//System.out.println(imgVec.size());

						if (imgVec.size() > 1) {
							synchronized (lock) {
								
								lock.wait();								
							}
						}

					} else {
						length = dataInputStream.readInt();
						dataInputStream.readFully(imageByte2, 0, length);
						screenImage = new BufferedImage(image_Width, image_Height, BufferedImage.TYPE_3BYTE_BGR);
						screenImage.setData(Raster.createRaster(screenImage.getSampleModel(),
								new DataBufferByte(imageByte2, imageByte2.length), new Point()));
					}
					//thread.sleep(20);
				}
				
			} catch (Exception e) {
				//DebugMessage.printDebugMessage(e);
			}

		}
	}

	class showThread extends Thread {
		byte imageByte[];
		byte uncompressImageByte[];		
		public void run() {
			
			while (true) {
				try {

					imageByte = imgVec.get(0);
					//System.out.println(imgVec.size());
					uncompressImageByte = Snappy.uncompress(imageByte);
					if (imgVec.size() > 0){
						imgVec.remove(0);
						//System.out.println(imgVec.size());
					}
					screenImage = new BufferedImage(image_Width, image_Height, BufferedImage.TYPE_3BYTE_BGR);
					screenImage.setData(Raster.createRaster(screenImage.getSampleModel(),
							new DataBufferByte(uncompressImageByte, uncompressImageByte.length), new Point()));					
				//	System.out.println(imgVec.size() + " " + thread.getState());
					if (imgVec.size() == 1) {
						synchronized (lock) {
							//System.out.println(thread.getState());							
								//System.out.println("notify~~~~~~~~");
								lock.notify();						
						}
					}
					if(screenImage != null){
						image = screenImage;
						FPScount++;
						repaint();
					}					
					
				} catch (Exception e) {

				}
				 // decompress(imageByte);
			}
		}
	}

	class FPSCheckThread extends Thread {
		public void run() {
			while (true) {
				try {
					sleep(1000);
					FPSlabel.setText("FPS : " + Integer.toString(FPScount));
					// repaint();
					// System.out.println("FPS : " + FPScount);
					FPScount = 0;
				} catch (InterruptedException e) {
				}
			}
		}
	}

	class KeyboardThread extends Thread {
		int result;
		int keypress = 0;

		public void run() {
			LowLevelKeyboardProc rr = new LowLevelKeyboardProc() {

				@Override
				public LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT info) {
					try {
						 System.out.println(info.vkCode);
						if (info.vkCode == 21) {
							System.out.println("한영");
							if (keypress == 0) {
								u32.keybd_event((byte) 0x15, (byte) 0, 0, 0);// 누름ffDDDddSS
								u32.keybd_event((byte) 0x15, (byte) 00, (byte) 0x0002, 0);// 땜
								keypress++;
							} else {
								keypress = 0;
							}

						}
						if (nCode >= 0) {

							switch (wParam.intValue()) {
							case WinUser.WM_KEYUP:
								keyboardOutputStream.writeInt(KEY_RELEASED);
								keyboardOutputStream.writeInt(info.vkCode);
								break;
							case WinUser.WM_KEYDOWN:
								keyboardOutputStream.writeInt(KEY_PRESSED);
								keyboardOutputStream.writeInt(info.vkCode);
								break;
							case WinUser.WM_SYSKEYUP:
								keyboardOutputStream.writeInt(KEY_RELEASED);
								keyboardOutputStream.writeInt(info.vkCode);
								break;
							case WinUser.WM_SYSKEYDOWN:
								keyboardOutputStream.writeInt(KEY_PRESSED);
								keyboardOutputStream.writeInt(info.vkCode);
								break;
							}

						}
					} catch (Exception e) {
						DebugMessage.printDebugMessage(e);
					}

					Pointer ptr = info.getPointer();

					long peer = Pointer.nativeValue(ptr);

					return lib.CallNextHookEx(hhk, nCode, wParam, new LPARAM(peer));
				}
			};
			hhk = lib.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, rr, hMod, 0);
			MSG msg = new MSG();
			while ((result = lib.GetMessage(msg, null, 0, 0)) != 0) {
				if (result == -1) {
					System.err.println("error in get message");
					break;
				} else {
					System.err.println("got message");
					lib.TranslateMessage(msg);
					lib.DispatchMessage(msg);
				}
			}
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
	}
	 class ChatListenerThread extends Thread {
	        public void run() {
	            try {
	                while (true) {
	                    String serverMessage = chatInputStream.readUTF();
	                    chatArea.append("Server: " + serverMessage + "\n");
	                }
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    // Khởi tạo thread lắng nghe tin nhắn chat
	    public void startChatListener() {
	        new ChatListenerThread().start();
	    }
}
