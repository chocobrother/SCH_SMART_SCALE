/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uclab.ui;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import uclab.algorithms.ECG_Signal;
import uclab.common.LogPrint;

/**
 *
 * @author train
 */
public class UiupdateThread implements Runnable{
    JFrame window;
    int x;
    ECG_Signal ecgSignal;
    XYSeries series;
    ValueAxis range;
    
    JLabel sqiLabel, weightLabel;
    private XYLineAndShapeRenderer renderer;
    private ImageIcon badSignal;
    private ImageIcon goodSignal;
    FileWriter ecgTextFile;   
    
    private BlockingQueue<Integer> bq_ecg;
    private BlockingQueue<Integer> bq_weight;
    
    private int[] autoScale;
    private int scaleCurrentIdx;
    
    private boolean flag = true;    
    public boolean stop_add_ecg = false;
    public boolean flag2 = true;
                
    public UiupdateThread(JFrame window, XYSeries series, ValueAxis range, JLabel sqiLabel, JLabel weightLabel, XYLineAndShapeRenderer renderer, 
            ECG_Signal ecgSignal, BlockingQueue<Integer> bq_ecg, BlockingQueue<Integer> bq_weight){
        
        //set Image size 
        try{
            BufferedImage img = ImageIO.read(new File("C:\\Users\\train\\Documents\\NetBeansProjects\\testNetBean\\res\\thumbs_down.png"));
            Image dimg = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            badSignal = new ImageIcon(dimg);
          
            img = ImageIO.read(new File("C:\\Users\\train\\Documents\\NetBeansProjects\\testNetBean\\res\\thumbs_up.png"));
            dimg = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            goodSignal = new ImageIcon(dimg);
            
        }catch(IOException e){
            e.printStackTrace();
        }
        
        this.window= window;
        this.ecgSignal = ecgSignal;
        this.series = series;
        this.sqiLabel = sqiLabel;
        this.weightLabel = weightLabel;
        this.renderer = renderer;
        this.bq_ecg = bq_ecg;
        this.bq_weight = bq_weight;
        this.x = 0;
        this.range = range;
        this.autoScale = new int[120];  /// length가 2초 정도의 데이터가 저장될 수 있을 정도의 사이즈
        this.scaleCurrentIdx = 0;
        this.stop_add_ecg = false;
        flag2 = true;
//        try{
//        ecgTextFile = new FileWriter("test.txt"); // 텍스트 파일이 없으면 새로 생성함!
////		reader.write("입출력!"); // 파일에 "입출력!"을 저장함.
////
////		reader.append('!'); // 파일의 끝에 ! 문자를 추가시킴.
////
////		reader.close(); // 파일을 닫음.\
//        }catch(IOException e){
//            e.printStackTrace();
//        }
    }

    public void threadStop(){
        System.out.println("시리얼 통신 종료");
        
        this.flag = false;
    }
        
    @Override
    public void run() {            
         //체중과 ecg로 나눠서 그리도록
         
         while (flag) {
            try {
                Thread.sleep(5);
                
                if(ecgSignal.getCleanSignalFlag()){
                        renderer.setSeriesPaint(0, Color.GREEN);
                        sqiLabel.setText("Good Signal");
                        sqiLabel.setIcon(goodSignal);
                }else{
                        renderer.setSeriesPaint(0, Color.RED);
                        sqiLabel.setText("Bad Signal");
                        sqiLabel.setIcon(badSignal);
                }
                    
                //range 구하기
                if(scaleCurrentIdx == autoScale.length){
                    //min
                    flag2 = false;
                    int minY = autoScale[0];
                    int maxY = autoScale[0];
                    
                   for(int n = 0; n < autoScale.length; n++){
                       if(minY > autoScale[n])
                           minY = autoScale[n];
                       
                       if(maxY < autoScale[n])
                           maxY = autoScale[n];
                   }
                   
                   range.setRange(minY-0.5, maxY+0.5);
                   scaleCurrentIdx = 0;
                }
                
                //queue가 비어있지 않으면
                if(!bq_ecg.isEmpty() && !stop_add_ecg){
                    
//                    System.out.println("ecg queue size:"+bq_ecg.size());
//      float Voltage0 = float(reading0) * 5 / 1023;
                    double value = (double)bq_ecg.take();
                    value =  value * 5 / 1023;
//                    LogPrint.println("UiupdateThread", "ecg : "+value);
                    ecgSignal.setSignalAdd(value);
                    series.add(x++, value);    
//                    if(scaleCurrentIdx < 120)
                    autoScale[scaleCurrentIdx++] = (int)value;
                }
                
                if(!bq_weight.isEmpty()){
                    //체중표시 
                    weightLabel.setText(bq_weight.take()+"KG");
//                    System.out.println("weight:"+bq_weight.take());
                }
//                if (inputStream.available() > 0) {                 
//                            
//                    window.repaint();
//                }
            } catch (NumberFormatException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }   
}
