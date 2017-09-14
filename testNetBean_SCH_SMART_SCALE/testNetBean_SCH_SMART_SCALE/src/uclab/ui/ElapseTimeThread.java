package uclab.ui;

import com.fazecast.jSerialComm.SerialPort;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import uclab.algorithms.ECG_Signal;
import uclab.common.LogPrint;

public class ElapseTimeThread implements Runnable{

	private ECG_Signal ecgSignal;
	private JLabel elapseTimeText, afDetectionText;
        private JButton button;
	private int timeText = 30;
	private int countSecond = 5;
	private boolean flag = true;
        private SerialPort chosenPort;
        
        private long startTime = System.currentTimeMillis();
	
        private boolean threadFlag = true;        
        private boolean waitFlag = false;
        
        private UiupdateThread uiUpdate;
        
	public ElapseTimeThread(ECG_Signal ecgSignal, JLabel elapseTimeText, JButton button, JLabel afDetectionText, SerialPort chosenPort, UiupdateThread uiUpdate){
                this.button = button;
		this.ecgSignal = ecgSignal;
		this.elapseTimeText = elapseTimeText;
		this.afDetectionText = afDetectionText;
                this.chosenPort = chosenPort;
                this.uiUpdate = uiUpdate;
        }
	
        public void threadStop(){
            LogPrint.println("ElapseTimeThread","ElapseTimeThread 종료");                                            

            this.threadFlag = false;
        }
        
        public void setWaitFlag(boolean flag){
            this.waitFlag = flag;
        }
            
        public boolean getWaitFlag(){
            return this.waitFlag;
        }
        
        public void threadNotify(){
            synchronized(this){
                this.notify();
            }
        }
                  
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		while(threadFlag){
//			System.out.println(ecgSignal.getElapseTimeFlag()+","+ecgSignal.getCleanSignalFlag());
                        synchronized(this){
                            try {
                                if(waitFlag)
                                    this.wait();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ElapseTimeThread.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
//                        long currentTime = System.currentTimeMillis();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

//                    if(currentTime - startTime > 1000){
//			if(ecgSignal.getCleanSignalFlag() && flag){
//현재 임시방편임. 완벽하게 하려면 콜백메소드를 구현해야한다.
			if(flag){		                   
                                if(timeText > 25 || ecgSignal.getCleanSignalFlag())
                                    elapseTimeText.setText((--timeText)+"s");
//                                LogPrint.println("ElapseTimeThread","진입");                                           
                                    
				if(timeText == 26){
                                    //심전도 달라는 패킷 전송                                
                                    OutputStream outputStream = chosenPort.getOutputStream();                 

                                    try {
                                        //ecg req packet 응답패킷을 받을 때까지 패킷을 전송함                                      
                                        while(!SerialCommunicationThread.getResponsePacket()){
                                            
                                            try {
                                                Thread.sleep(5);
                                                } catch (InterruptedException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            }
                                            
                                            outputStream.write(51);
                                            
                                            LogPrint.println("ElapseTimeThread","심전도 요청 패킷 전송");                                                           
                                        }
                                            LogPrint.println("ElapseTimeThread","심전도 요청에 대한 응답 패킷 받음");                                                     
                                    } catch (IOException ex) {
                                        Logger.getLogger(MainJFrame.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    
                                    afDetectionText.setText("심전도 측정 중...");
                                }
                                
				if(timeText == 0){               
                                    uiUpdate.stop_add_ecg = true;
                                    LogPrint.println("ElapseTImeThread", "ElapseTime 0s");
                                    afDetectionText.setText("부정맥 진단 중");
                                    String result_af = ecgSignal.afDetection();
					if(result_af.equals("yes")){
                                                LogPrint.println("ElapseTimeThread", "af result: yes");
						BufferedImage img = null;
						try {
							img = ImageIO.read(new File("C:\\Users\\train\\Documents\\NetBeansProjects\\testNetBean\\res\\afdetection_image.png"));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Image dimg = img.getScaledInstance(200, 100, Image.SCALE_SMOOTH);
						afDetectionText.setIcon(new ImageIcon(dimg));                                                
                                                afDetectionText.setText(" ");
					}else if(result_af.equals("no")){
                                                LogPrint.println("ElapseTImeThread", "af result: no");
						BufferedImage img = null;
//						flag = true;
						try {
							img = ImageIO.read(new File("C:\\Users\\train\\Documents\\NetBeansProjects\\testNetBean\\res\\nsrimae.png"));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Image dimg = img.getScaledInstance(200, 100, Image.SCALE_SMOOTH);
						afDetectionText.setIcon(new ImageIcon(dimg));	
                                                afDetectionText.setText(" ");                                                
					}else{
                                            afDetectionText.setText("신호를 더 측정해야 합니다.");
                                        }
                                    
                                    ResultJFrame frame = new ResultJFrame(ecgSignal,result_af);
                                    frame.setVisible(true);
//					afDetectionText.setText(ecgSignal.afDetection());

//                                        try{
//                                            BufferedImage img = ImageIO.read(new File("C:\\Users\\train\\Documents\\NetBeansProjects\\testNetBean\\res\\button_1.png"));
//                                            Image dimg = img.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
//                                            ImageIcon startImage = new ImageIcon(dimg);
//                                            button.setIcon(startImage);
//                                            button.setText(" START");
//                                        }catch(IOException e){
//                                        }
                                        
					flag = false;		
//					ecgSignal.clean_signal.clear();                                                                                                       
				}
//			}
//                        startTime = currentTime;
		}
            }
	}
}
