package com.mostc.pftt.util;

import java.util.concurrent.atomic.AtomicBoolean;

public final class TimerUtil {
	
	public static boolean trySleepSeconds(int seconds) {
		return trySleepMillis(seconds*1000);
	}
	
	public static boolean trySleepMillis(int millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch ( Exception ex ) {}
		return false;
	}
	
	public interface RepeatingRunnable {
		public void run(RepeatingThread thread);
	}

	public static RepeatingThread repeatEverySeconds(int seconds, RepeatingRunnable r) {
		RepeatingThread t = new RepeatingThread(seconds, r);
		t.start();
		return t;
	}
	
	public static class RepeatingThread extends RepeatingOrTimingThread {
		protected final RepeatingRunnable r;
		
		public RepeatingThread(int seconds, RepeatingRunnable r) {
			super(seconds);
			this.r = r;
		}
		
		@Override
		public void run() {
			while(true) {
				doWait();
				if (isCancelled())
					return;
				
				r.run(this);
			}
		}
		
	} // end public static class RepeatingThread
	
	public static TimerThread2S waitSeconds(int seconds1, Runnable r1, int seconds2, Runnable ...runnables2) {
		TimerThread2S t;
		if (runnables2.length==1) {
			t = new TimerThread2S1(seconds1, r1, seconds2, runnables2[0]);
		} else if (runnables2.length==2) {
			t = new TimerThread2S2(seconds1, r1, seconds2, runnables2[0], runnables2[1]);
		} else {
			t = new TimerThread2SX(seconds1, r1, seconds2, runnables2);
		}
		t.start();
		return t;
	}
	
	public static class TimerThread2S1 extends TimerThread2S {
		protected final Runnable r2;

		public TimerThread2S1(int seconds, Runnable r1, int seconds2, Runnable r2) {
			super(seconds, r1, seconds2);
			this.r2 = r2;
		}
		
		@Override
		protected void fire2() {
			r2.run();
		}
		
	} // end public static class TimerThread2S1
	
	public static class TimerThread2S2 extends TimerThread2S {
		protected final Runnable a2, b2;

		public TimerThread2S2(int seconds, Runnable r1, int seconds2, Runnable a2, Runnable b2) {
			super(seconds, r1, seconds2);
			this.a2 = a2;
			this.b2 = b2;
		}
		
		@Override
		protected void fire2() {
			new Thread() {
				public void run() {
					b2.run();
				}
			}.start();
			a2.run();
		}
		
	} // end public static class TimerThread2S2

	public static class TimerThread2SX extends TimerThread2S {
		protected final Runnable[] runnables2;
		
		public TimerThread2SX(int seconds, Runnable r1, int seconds2, Runnable[] runnables2) {
			super(seconds, r1, seconds2);
			this.runnables2 = runnables2;
		}
		
		@Override
		protected void fire2() {
			if (runnables2.length==0)
				return;
			
			for ( int i=1 ; i < runnables2.length ; i++ ) {
				final Runnable r = runnables2[i];
				new Thread() {
						@Override
						public void run() {
							r.run();
						}
					}.start();
			}
			runnables2[0].run();
		}
		
	} // end public static class TimerThread2SX
	
	public static abstract class TimerThread2S extends TimerThread {
		protected final int seconds2;
		protected final Runnable r1;
		
		public TimerThread2S(int seconds, Runnable r1, int seconds2) {
			super(seconds);
			this.r1 = r1;
			this.seconds2 = seconds2 - seconds;
		}

		@Override
		public void run() {
			doWait();
			if (isCancelled())
				return;
			
			fire1();
			
			doWait2();
			if (isCancelled())
				return;
			
			fire2();
		}
		
		protected void doWait2() {
			sleeping.set(true);
			try {
				Thread.sleep(seconds2*1000);
			} catch ( InterruptedException ex ) {
				// get interrupt from #cancel and then stop waiting
			}
			sleeping.set(false);
		}
		
		protected void fire1() {
			r1.run();
		}
		
		protected abstract void fire2();
		
	} // end public static abstract class TimerThread2S
	
	public static TimerThread1S waitSeconds(int seconds, Runnable ...runnables) {
		TimerThread1S t;
		if (runnables.length==1) {
			t = new TimerThread1(seconds, runnables[0]);
		} else if (runnables.length==2) {
			t = new TimerThread2(seconds, runnables[0], runnables[1]);
		} else {
			t = new TimerThreadX(seconds, runnables);
		}
		t.start();
		return t;
	}
	
	public static class TimerThread1 extends TimerThread1S {
		protected final Runnable r;

		public TimerThread1(int seconds, Runnable r) {
			super(seconds);
			this.r = r;
		}
		
		@Override
		protected void fire() {
			r.run();
		}
		
	} // end public static class TimerThread1
	
	public static class TimerThread2 extends TimerThread1S {
		protected final Runnable a, b;

		public TimerThread2(int seconds, Runnable a, Runnable b) {
			super(seconds);
			this.a = a;
			this.b = b;
		}
		
		@Override
		protected void fire() {
			new Thread() {
				public void run() {
					b.run();
				}
			}.start();
			a.run();
		}
		
	} // end public static class TimerThread2

	public static class TimerThreadX extends TimerThread1S {
		protected final Runnable[] runnables;
		
		public TimerThreadX(int seconds, Runnable[] runnables) {
			super(seconds);
			this.runnables = runnables;
		}
		
		@Override
		protected void fire() {
			if (runnables.length==0)
				return;
			
			for ( int i=1 ; i < runnables.length ; i++ ) {
				final Runnable r = runnables[i];
				new Thread() {
						@Override
						public void run() {
							r.run();
						}
					}.start();
			}
			runnables[0].run();
		}
		
	} // end public static class TimerThreadX
	
	public static abstract class TimerThread1S extends TimerThread {
		
		public TimerThread1S(int seconds) {
			super(seconds);
		}

		@Override
		public void run() {
			doWait();
			if (isCancelled())
				return;
			
			fire();
		}
		
		protected abstract void fire();
		
	} // end public static abstract class TimerThread1S
	
	public static abstract class TimerThread extends RepeatingOrTimingThread {

		public TimerThread(int seconds) {
			super(seconds);
		}
		
	}
	
	protected static class RepeatingOrTimingThread extends Thread {
		protected final int seconds;
		protected final AtomicBoolean b, sleeping;
		
		public RepeatingOrTimingThread(int seconds) {
			this.seconds = seconds;
			
			b = new AtomicBoolean(false);
			sleeping = new AtomicBoolean(false);
			setName("Timer"+getName());
			setDaemon(true);
		}
		
		public void cancel() {
			b.set(true);
			if (sleeping.get()) {
				// interrupt #sleep not #fire
				RepeatingOrTimingThread.this.interrupt();
			}
		}
		
		public boolean isCancelled() {
			return b.get();
		}
		
		protected void doWait() {
			sleeping.set(true);
			try {
				Thread.sleep(seconds*1000);
			} catch ( InterruptedException ex ) {
				// get interrupt from #cancel and then stop waiting
			}
			sleeping.set(false);
		}
		
	} // end protected static class RepeatingOrTimingThread
	
	private TimerUtil() {}
	
} // end public final class TimerUtil
