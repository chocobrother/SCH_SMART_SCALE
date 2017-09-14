package uclab.algorithms;

import java.awt.Label;
import java.util.ArrayList;

import javax.swing.JLabel;
import uclab.Common.TextFileWrite;
import uclab.common.LogPrint;

/**
 * Created by sun on 2017-02-04.
 */
public class ECG_Signal implements Runnable {
	private final String TAG = "HEART_RATE_THREAD";
	private final int GoodSignal = 1, BadSignal = 2;
	long startTime, currentTime, signaTestTime;
	long checkTime = 0;
	boolean signalClean = false;
	boolean cleanSignalFlag = false;
	boolean elapseTimeFlag = false;
	boolean weightFlag = false;
        
        boolean threadFlag = true;
        
	String afDetection = "No";
	public ArrayList<Double> full_signal, clean_signal;
	// ?��?��?��?��?��굨占?��괠筌ｌ꼶�쓥
	int[] heartRateArray;
	int hrArrayIdx = 0;
	int fs_min = 90;

	// add text
	JLabel heartRate;
	JLabel sqi;

	peakDetection peak;
	// hrThread hr;//
	// ECG_Manage_Thread dataMange;

	ArrayList<Double> avgHr = new ArrayList<Double>();
	double[] a, b;
	ArrayList<Double> A = new ArrayList<Double>();
	ArrayList<Double> B = new ArrayList<Double>();

	int fileNumber = 0, fileNumber1 = 0;

        TextFileWrite<Double> textFile, filteredFile;
        
	public ECG_Signal(JLabel heartRate, JLabel sqi) {
//                textFile = new TextFileWrite<Double>("rawSignalFileForder", "rawEcg");
//                filteredFile = new TextFileWrite<Double>("filteredFileForder", "filteredEcg");
                
		this.heartRate = heartRate;
		this.sqi = sqi;
	
		full_signal = new ArrayList<Double>();
		clean_signal = new ArrayList<Double>();
		heartRateArray = new int[2];

		a = new double[7];
		b = new double[7];

		a[0] = 0.0614;
		a[1] = 0.0;
		a[2] = -0.1841;
		a[3] = 0.0;
		a[4] = 0.1841;
		a[5] = 0.0;
		a[6] = -0.0614;

		b[0] = 1.0;
		b[1] = -2.3648;
		b[2] = 2.9556;
		b[3] = -2.4505;
		b[4] = 1.4848;
		b[5] = -0.5514;
		b[6] = 0.1108;

		for (int n = 0; n < a.length; n++)
			A.add(a[n]);
		for (int n = 0; n < b.length; n++)
			B.add(b[n]);
	}

	public void setSignalAdd(double value) {
		full_signal.add(value);

		if (cleanSignalFlag) {
			clean_signal.add(value);
		}
	}

	public void start() {
		startTime = System.currentTimeMillis();
		signaTestTime = System.currentTimeMillis();
	}

	public double getAvgHr() {
		double tmp = 0;
		for (int n = 0; n < avgHr.size(); n++)
			tmp += avgHr.get(n);

		tmp /= avgHr.size();

		return tmp;
	}

	public String afDetection() {	
		ArrayList<Double> data = new ArrayList<>();
//
//                for(int n = 0; n < clean_signal.size(); n++){
//                    textFile.add(clean_signal.get(n));
//                }
                
//		if (clean_signal.size() < fs_min * 10) {
//			return "--";
//		}

//		for (int n = 0; n < (10 * fs_min); n++) {
//			data.add(clean_signal.get(n));
//		}

                for(int n = fs_min*5; n < fs_min*15; n++){
                    data.add(clean_signal.get(n));
                }
		
		ArrayList<Double> filtTmp = Filtfilt.doFiltfilt(A, B, data);
                                
		double[] ecg_h = new double[filtTmp.size()];               
                              
//		 double[] ecg_h = bandpassfilter.calculation(b, a, data,
//		 data.length);

		for(int n = 0; n < filtTmp.size(); n++){
			ecg_h[n] = filtTmp.get(n);
		}
		
//                for(int n = 0; n < ecg_h.length; n++){
////                    System.out.println("filteredECG:"+ecg_h[n]);
//                    filteredFile.add(ecg_h[n]);
//                }
                                
		ecg_h = cus_math.max_division(ecg_h);

		peakDetection peak = new peakDetection(ecg_h, fs_min);

		peak.calculation();

		ArrayList<Integer> tmp = peak.getRpeak();

		double[] rr = new double[tmp.size() - 1];
		double[] rri = new double[rr.length];
		// diff
		for (int n = 0; n < rr.length; n++) {
			rr[n] = tmp.get(n + 1) - tmp.get(n);
		}

		for (int n = 0; n < rr.length; n++) {
			rri[n] = rr[n] / 60;
		}

		// rmssd
		double dd = 0;
		for (int n = 0; n < rri.length - 1; n++) {
			dd += Math.pow(rri[n + 1] - rri[n], 2);
		}

		double rmssd = Math.sqrt(dd / rri.length);

                LogPrint.println("ECG_Signal", "rmssd: "+rmssd);
//		System.out.println("rmssd : " + rmssd);

		if (rmssd > 0.3) {
			afDetection = "yes";
		} else {
			afDetection = "no";
		}

		return afDetection;
	}

	public double hearRate(ArrayList<Integer> data, int fs) {
		int[] rr = cus_math.RR_interval(data);
		int hr = 0;

		for (int n = 0; n < rr.length; n++) {
			hr += (fs * 60) / rr[n];
			// Log.e(TAG, "HR: " + (fs * 60) / rr[n] + ", RR size:" + rr.length
			// + ", fs:" + fs);
		}
		hr /= rr.length;

		return hr;
	}

	public boolean isSignalClean() {
		// Log.e(TAG, "full_signal: "+full_signal.size());

		if (full_signal.size() > (fs_min * 2)) {
			// textFile.TextFileInit(textFile.getPath(), "ecg"+(fileNumber++));

//			double[] data = new double[full_signal.size()];
			ArrayList<Double> data = new ArrayList<>();
			int length = full_signal.size();
			// ?��?��뵠�?��?���젟?��?��?��?��?��?��?��?���? full_signal?��?��?�� ?��?��?��?��?��뵠占?��?��?��?��?�� ?��?��굶占?��?��?��?��?���⑨?��
			// ?��?��뿳占?��몵沃?��?��?��?��?�� data.length�몴?���? ?��?��?��?��?��?��?��?��?��?��?��?�� ?��?���?
			for (int n = 0; n < length; n++) {
				data.add(full_signal.get(n));
			}
//			for (int n = 0; n < data.length; n++) {
//				data[n] = full_signal.get(n);
//			}

			int fs = data.size() / 2;
		
			ArrayList<Double> filtTmp = Filtfilt.doFiltfilt(A, B, data);
			double[] ecg_h = new double[filtTmp.size()];                       
                        
//			 double[] ecg_h = bandpassfilter.calculation(b, a, data,
//			 data.length);

			for(int n = 0; n < filtTmp.size(); n++){
				ecg_h[n] = filtTmp.get(n);
			}
			
			 ecg_h = cus_math.max_division(ecg_h);
			 peakDetection peak = new peakDetection(ecg_h, fs);
//			data = cus_math.max_division(data);
//			peakDetection peak = new peakDetection(data, fs);
			peak.calculation();

			ArrayList<Integer> tmp = peak.getRpeak();

			double hr = 0;
			if (tmp.size() > 1) {
				fs=100;
				hr = hearRate(tmp, fs);
				heartRate.setText("" + (int) hr);
				// signalHeartRateySendMessage((int)hr);
				// Log.e(TAG, "Heart Rate " + hr);
				heartRateArray[hrArrayIdx++] = (int) hr;

				if (hrArrayIdx == 2) {
					hrArrayIdx = 0;
				}

				double tmp1 = heartRateArray[0] - heartRateArray[1];
                                LogPrint.println("ECG_SIGNAL","Heart Rate " + hr +",heartRateArray[0]:"+heartRateArray[0]+",heartRateArray[1]"+heartRateArray[1]);    				
				if (hr > 150 || hr < 50) {
					return false;
				}

				if (Math.abs(tmp1) > 30) {
					// bad signal
					return false;
				} else {
					// good signal
					avgHr.add(hr);
					return true;
				}
			}
		}
		return false;
	}
	
	public void setElapseTimeFlag(boolean flag){
		elapseTimeFlag = flag;
	}
	
	public boolean getElapseTimeFlag(){
		return elapseTimeFlag;
	}
	
	public boolean getCleanSignalFlag(){
		return cleanSignalFlag;
	}

        public void threadStop(){
            LogPrint.println("ECG_SIGNAL","ecgsignal thread 종료");    		            

            this.threadFlag = false;
        }
                    
	@Override
	public void run() {
		boolean heartRateFlag = false;

		while (threadFlag) {
			currentTime = System.currentTimeMillis();
			if (currentTime - startTime >= 2000) {
				// Log.e(TAG, "current:---");
//				full_signal.clear();
                                LogPrint.println("ECG_Signal", "fs:"+full_signal.size()/2);
                                
				if (isSignalClean()) {
					checkTime += 2000;
					cleanSignalFlag = true;
					
					//ElapseTread�? ?��?��?��?���? ?��?��?��
//					if(!elapseTimeFlag){
//						System.out.println("true check");
//						elapseTimeFlag = true;
//					}
//					sqi.setText("Good Signal");
					
					// signalQualitySendMessage(GoodSignal);//good signal
				} else {
					// signalQualitySendMessage(BadSignal);//bad signal
//					sqi.setText("Bad Signal");
					cleanSignalFlag = false;
					checkTime = 0;
				}

				full_signal.clear();
				startTime = currentTime;
			}
		}
	}
}
