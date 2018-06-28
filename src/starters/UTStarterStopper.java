package starters;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Simple GUI application to start and stop all UT software modules: 1)
 * MechioZenoRobotControllerStarter, 2) ZenoRealizerStarter, 3)
 * FlipperDialogStarter. This GUI shows 2 buttons: 1) Start all modules and 2)
 * Stop all modules. The start button starts the 3 aforementioned modules in new
 * seperate java processes, while caching the process handle. The stop button
 * uses the cached process handle to attempt a graceful shutdown, or if that
 * fails it attempts a forceful shutdown
 * 
 * @author davisond
 *
 */
@SuppressWarnings("serial")
public class UTStarterStopper extends JFrame implements ActionListener {

	ReentrantLock lock = new ReentrantLock();

	JButton bStart;
	JButton bStop;

	JLabel msg;

	Process mechIOProcess;
	Process asapProcess;
	Process flipperProcess;

	private enum Actions {
		START, STOP
	}

	/**
	 * Creates the GUI with 2 buttons Registers a windows close event listener
	 * so we can shut down any running processes first!
	 * 
	 * @param title
	 *            title of the GUI window
	 */
	UTStarterStopper(String title) {
		super(title);

		this.setContentPane(buildGUI());
		this.pack();

		// register a new handler for the closing event, so we can first cleanly
		// shut down all spawned processes
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				if (JOptionPane.showConfirmDialog(UTStarterStopper.this,
						"Closing this window will stop all running UT software", "Are you sure..?",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							Thread stopperThread = stopAll();
							try {
								stopperThread.join();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							System.exit(0);
						}
					}).start();
				}
			}
		});
		
		//and a hook for catching CTRL-C
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
	        public void run() {
				Thread stopperThread = stopAll();
				try {
					stopperThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	        }
	    });
	}

	/**
	 * Creates and fills a JPanel with all the GUI interface elements
	 * 
	 * @return a JPanel with all GUI elements
	 */
	public JPanel buildGUI() {
		JPanel jp = new JPanel();
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		jp.setBorder(new EmptyBorder(new Insets(22, 22, 22, 22)));
		jp.add(Box.createVerticalGlue());

		msg = new JLabel("", SwingConstants.CENTER);
		msg.setSize(200, 100);
		msg.setAlignmentX(Component.CENTER_ALIGNMENT);
		jp.add(msg);

		bStart = new JButton("Start all modules");
		bStart.setAlignmentX(Component.CENTER_ALIGNMENT);
		bStart.setActionCommand(Actions.START.name());
		bStart.addActionListener(this);
		bStart.setEnabled(true);
		jp.add(bStart);

		bStop = new JButton("Stop all modules");
		bStop.setAlignmentX(Component.CENTER_ALIGNMENT);
		bStop.setActionCommand(Actions.STOP.name());
		bStop.addActionListener(this);
		bStop.setEnabled(false);
		jp.add(bStop);

		jp.add(Box.createVerticalGlue());

		return jp;
	}

	/**
	 * Shows a text message on the GUI.. This is always executed on the main UI
	 * thread
	 * 
	 * @param message
	 *            the message to show
	 */
	private void showMessage(String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				System.out.println(message);
				msg.setText("<html>" + message + "</html>");
				msg.repaint();
				getContentPane().repaint();
				repaint();
			}
		});
	}

	/**
	 * Starts all UT software modules, each in it's own independent process
	 * Stores the process handles for later use
	 */
	public Thread startAll() {
		Thread starterThread = new Thread(new Runnable() {
			@Override
			public void run() {
				lock.lock();
				System.out.println("Starting all modules...");
				try {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							bStart.setEnabled(false);
							bStop.setEnabled(false);
						}
					});
					
					startMechIO();
					Thread.sleep(5000);
					startASAP();
					Thread.sleep(5000);
					startFlipper();
					Thread.sleep(5000);
					showMessage("");

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							bStart.setEnabled(false);
							bStop.setEnabled(true);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
			}
		});

		starterThread.start();
		return starterThread;
	}

	private void startMechIO() throws IOException {
		showMessage("Starting MechIO....");
		if ((mechIOProcess == null || !mechIOProcess.isAlive())) {
			mechIOProcess = startJavaProcess("starters.MechioZenoRobotControllerStarter");
		}
	}

	private void startASAP() throws IOException {
		showMessage("Starting ASAP....");
		if ((asapProcess == null || !asapProcess.isAlive())) {
			asapProcess = startJavaProcess("starters.ZenoRealizerStarter");
		}
	}

	private void startFlipper() throws IOException {
		showMessage("Starting Flipper....");
		if ((flipperProcess == null || !flipperProcess.isAlive())) {
			flipperProcess = startJavaProcess("starters.FlipperDialogStarter");
		}
	}

	/**
	 * Starts the given main method in a new java process and returns the
	 * process handle
	 * 
	 * @param main
	 *            the main method to be run
	 * @return a process handle to the new java process
	 * @throws IOException
	 *             when something goes wrong
	 */
	private Process startJavaProcess(String main) throws IOException {
		// java -cp "build/classes/:lib/*:resource" -Djava.library.path=lib
		// starters.ZenoRealizerStarter
		List<String> args = new ArrayList<String>();
		args.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
		args.add("-cp");
		args.add("\"build/classes" + File.pathSeparator + System.getProperty("java.class.path") + "\"");
		args.add("-Dlogback.configurationFile=logconfig.xml");
		args.add("-Djava.library.path=lib");
		args.add(main);
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.inheritIO();
		return pb.start();
	}

	/**
	 * Stops any currently running UT processes
	 */
	public Thread stopAll() {
		Thread stopperThread = new Thread(new Runnable() {
			@Override
			public void run() {
				lock.lock();
				System.out.println("Stopping all modules....");
				try {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							bStart.setEnabled(false);
							bStop.setEnabled(false);
						}
					});
					
					stopMechIO();
					stopASAP();
					stopFlipper();
					showMessage("");

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							bStart.setEnabled(true);
							bStop.setEnabled(false);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
			}
		});

		stopperThread.start();
		return stopperThread;
	}

	private void stopMechIO() throws InterruptedException {
		if (mechIOProcess != null && mechIOProcess.isAlive()) {
			showMessage("Attempting to stop MechIO gracefully....");
			mechIOProcess.destroy();
			Thread.sleep(1000);
			if (mechIOProcess.isAlive()) {
				showMessage("Attempting to stop MechIO forcefully....");
				mechIOProcess.destroyForcibly();
				mechIOProcess = null;
				Thread.sleep(1000);
			}
		}
	}

	private void stopASAP() throws InterruptedException {
		if (asapProcess != null && asapProcess.isAlive()) {
			showMessage("Attempting to stop ASAP gracefully....");
			asapProcess.destroy();
			Thread.sleep(1000);
			if (asapProcess.isAlive()) {
				showMessage("Attempting to stop ASAP forcefully....");
				asapProcess.destroyForcibly();
				asapProcess = null;
				Thread.sleep(1000);
			}
		}
	}

	private void stopFlipper() throws InterruptedException {
		if (flipperProcess != null && flipperProcess.isAlive()) {
			showMessage("Attempting to stop Flipper gracefully....");
			flipperProcess.destroy();
			Thread.sleep(1000);
			if (flipperProcess.isAlive()) {
				showMessage("Attempting to stop Flipper forcefully....");
				flipperProcess.destroyForcibly();
				flipperProcess = null;
				Thread.sleep(1000);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (Actions.START.name().equals(e.getActionCommand())) {
			startAll();
		} else if (Actions.STOP.name().equals(e.getActionCommand())) {
			stopAll();
		}
		repaint();
	}

	public static void main(String[] args) {
		System.out.println(System.getProperty("java.class.path"));
		UTStarterStopper frm = new UTStarterStopper("Start and stop UT modules");
		frm.setSize(400, 200);
		frm.setVisible(true);
		
		for(String arg : args){
			if("--autorun".equals(arg)){
				frm.startAll();
			}
		}
	}

}
