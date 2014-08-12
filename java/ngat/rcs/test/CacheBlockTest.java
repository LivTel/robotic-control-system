/**
 * 
 */
package ngat.rcs.test;

import ngat.util.BlockingQueue;


/**
 * @author eng
 *
 */
public class CacheBlockTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final BlockingQueue q;
		
		q = new BlockingQueue();
		
		Runnable r1 = new Runnable() {			
			public void run() {
				int i = 0;
				while (true) {
					i++;
					try {Thread.sleep(30000L);} catch (InterruptedException ix) {}					
					System.err.println("Try push: "+i);
					try {
					q.push(new String(""));
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.err.println("Push done: "+i);
				}
				
			}
			
		};
		
		Runnable r2 = new Runnable() {
			public void run() {			
				while (true) {
					try {Thread.sleep(1000L);} catch (InterruptedException ix) {}				
					try {				
						System.err.println("Try get");
						q.remove();
						System.err.println("Got one, snoozing");
						try {Thread.sleep(60000L);} catch (InterruptedException ix) {}		
						System.err.println("Snoozed");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}			
		};

		Thread t1 = new Thread(r1);
		Thread t2 = new Thread(r2);
		
		t1.start();
		t2.start();
		
	}

}
