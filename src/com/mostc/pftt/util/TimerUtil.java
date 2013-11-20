package com.mostc.pftt.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.results.ConsoleManager;

public final class TimerUtil {
	 
	private static LinkedList<ThreadRunnableProxy> trp_pool = new LinkedList<ThreadRunnableProxy>();
	static {
		final int pool_size = Math.min(16, LocalHost.getInstance().getCPUCount() * 2);
		for ( int i = 0 ; i < pool_size ; i++ ) {
			ThreadRunnableProxy trp = new ThreadRunnableProxy();
			new Thread(trp).start();
				synchronized(trp_pool) {
					trp_pool.add(trp);
				}
		}
	}
	
	private static class ThreadRunnableProxy implements Runnable {
		private Runnable r;
		private final Object lock = new Object();
		private Thread thread;
		private boolean run = true;
		
		private void setRunnable(Runnable r) {
			this.r = r;
			synchronized(lock) {
				lock.notify();
			}
		}
		
		@SuppressWarnings("unused")
		private void stop() {
			run = false;
		}
		
		public void run() {
			thread = Thread.currentThread();
			while(run) {
				if (r!=null) {
					try {
						r.run();
					} catch (Throwable t) {
						thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
					}
					r = null;
					synchronized(trp_pool) {
						trp_pool.add(this);
					}
				}
				try {
					synchronized(lock) {
						lock.wait();
					}
				} catch ( Throwable t ) {
					try {
						Thread.sleep(100);
					} catch ( InterruptedException i ) {}
				}
			}
		}
	} // end private static class ThreadRunnableProxy
	
	/** Windows, at least, has problems creating threads late in some test runs sometimes.
	 * 
	 * Creating a thread requires getting a handle, and sometimes if Windows has allocated too
	 * many handles, it will wait a long time before allocating more, which delays thread creation
	 * (which in turn can delay things like killing off timed out processes, which in turn frees up handles).
	 * 
	 * Instead, some threads are preallocated in a pool at startup to help ensure that a thread
	 * can be created when its needed.
	 * 
	 * Note: you may not call #start or #setDaemon on the returned Thread. you will get an IllegalThreadStateException if you do.
	 * 
	 * @param r
	 * @return
	 */
	public static Thread runThread(Runnable r) {
		try {
			Thread t = new Thread(r);
			t.start();
			return t;
		} catch ( Throwable t ) {}
		ThreadRunnableProxy trp = null;
		try {
			synchronized(trp_pool) {
				if (!trp_pool.isEmpty())
					trp = trp_pool.removeLast();
			}
		} catch ( NoSuchElementException e ) {}
		if (trp==null) {
			// try again
			Thread t = new Thread(r);
			t.start();
			return t;
		} else {
			trp.setRunnable(r);
			return trp.thread;
		}
	}
	
	public static Thread runThread(String prefix, Runnable r) {
		Thread t = runThread(r);
		t.setName(prefix);
		return t;
	}
	
	public interface ObjectRunnable<E extends Object> {
		E run() throws Exception;
	}
	
	protected static final UncaughtExceptionHandler IGNORE = new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread arg0, Throwable arg1) {
			}
		};
	
	public static <E extends Object> WaitableRunnable<E> runWaitSeconds(String name_prefix, int seconds, ObjectRunnable<E> or) {
		WaitableRunnable<E> wr = new WaitableRunnable<E>(or);
		try {
			Thread t = runThread(name_prefix, wr);
			t.setUncaughtExceptionHandler(IGNORE);
			wr.t = t;
			wr.block(seconds);
		} catch ( Throwable t ) {
			t.printStackTrace();
		}
		return wr;
	}
	
	public static class WaitableRunnable<E extends Object> implements Runnable, IClosable {
		protected Exception ex;
		protected final ObjectRunnable<E> or;
		protected E result;
		protected Thread t;
		protected boolean ran;
		protected final Object lock = new Object();
		
		protected WaitableRunnable(ObjectRunnable<E> or) {
			this.or = or;
		}
		
		public void run() {
			try {
				result = or.run();
			} catch ( Exception ex ) {
				this.ex = ex;
			}
			unlock(true);
		}
		
		protected void block(int seconds) {
			int r = 0;
			int m = ( seconds * 1000 );
			while (ran && m > r) {
				try {
					Thread.sleep(100);
				} catch ( InterruptedException ex ) {
					break;
				}
				r += 100;
			}
			m = m - r;
			if (m>0) {
			synchronized(lock) {
				if (this.ran==false) {
					try {
						lock.wait(m);
					} catch ( Exception ex ) {}
				}
			}
			}
		}
		
		protected void unlock(boolean ran) {
			synchronized(lock) {
				this.ran = ran;
				lock.notify();
			}
		}
		
		@SuppressWarnings("deprecation")
		public void close() {
			unlock(false);
			t.stop(new RuntimeException());
		}
		
		public E getResult() {
			return result;
		}
		
		public boolean isFinished() {
			return !ran;
		}
		
		public boolean didRun() {
			return ran;
		}
		
		public Exception getException() {
			return ex;
		}
		
		@Override
		public void close(ConsoleManager cm) {
			close();
		}
		
	}
	
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
	
	protected static void createRThread(RepeatingOrTimingThread t) {
		t.t = runThread("Timer", (Runnable)t);
	}

	public static RepeatingThread repeatEverySeconds(int seconds, RepeatingRunnable r) {
		RepeatingThread t = new RepeatingThread(seconds, r);
		createRThread(t);
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
				if (isClosed())
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
		createRThread(t);
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
			runThread(new Runnable() {
				public void run() {
					b2.run();
				}
			});
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
				runThread(new Runnable() {
						@Override
						public void run() {
							r.run();
						}
					});
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
			if (isClosed())
				return;
			
			fire1();
			
			doWait2();
			if (isClosed())
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
		createRThread(t);
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
			runThread(new Runnable() {
				public void run() {
					b.run();
				}
			});
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
				runThread(new Runnable() {
						@Override
						public void run() {
							r.run();
						}
					});
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
			if (isClosed())
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
	
	protected static abstract class RepeatingOrTimingThread implements Runnable, IClosable {
		protected final int seconds;
		protected final AtomicBoolean b, sleeping;
		protected Thread t;
		
		public RepeatingOrTimingThread(int seconds) {
			this.seconds = seconds;
			
			b = new AtomicBoolean(false);
			sleeping = new AtomicBoolean(false);
		}
		
		public void close() {
			b.set(true);
			if (sleeping.get()) {
				// interrupt #sleep not #fire
				t.interrupt();
			}
		}
		
		public boolean isClosed() {
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
		
		@Override
		public void close(ConsoleManager cm) {
			close();
		}
		
	} // end protected static class RepeatingOrTimingThread
	
	private TimerUtil() {}
	
} // end public final class TimerUtil
