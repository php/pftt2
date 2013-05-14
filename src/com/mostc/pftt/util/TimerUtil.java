package com.mostc.pftt.util;

import java.util.concurrent.atomic.AtomicBoolean;

public final class TimerUtil {

	public static TimerThread waitSeconds(int seconds, Runnable ...runnables) {
		TimerThread t;
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
	
	public static class TimerThread1 extends TimerThread {
		protected final Runnable r;

		public TimerThread1(int seconds, Runnable r) {
			super(seconds);
			this.r = r;
		}
		
		@Override
		protected void fire() {
			r.run();
		}
		
	}
	
	public static class TimerThread2 extends TimerThread {
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
		
	}

	public static class TimerThreadX extends TimerThread {
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
		
	}
	
	public static abstract class TimerThread extends Thread {
		protected final int seconds;
		protected final AtomicBoolean b;
		
		public TimerThread(int seconds) {
			this.seconds = seconds;
			
			b = new AtomicBoolean(false);
			setName("Timer"+getName());
		}
		
		public void cancel() {
			b.set(true);
		}
		
		public boolean isCancelled() {
			return b.get();
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(seconds*1000);
			} catch ( InterruptedException ex ) {
				// TODO get interrupt from #cancel and then stop waiting
				ex.printStackTrace();
			}
			if (isCancelled())
				return;
			
			fire();
		}
		
		protected abstract void fire();
	}
	
	private TimerUtil() {}
	
} // end public final class TimerUtil
