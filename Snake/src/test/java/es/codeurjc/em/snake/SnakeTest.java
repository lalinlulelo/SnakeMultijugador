package es.codeurjc.em.snake;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.websocket.DeploymentException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SnakeTest {
	
	static Thread threadServer = new Thread(() -> iniciarServerHilo());
	static volatile Semaphore server = new Semaphore(0);
	
	static volatile Semaphore finish = new Semaphore(0);
	static volatile Semaphore createdRoom = new Semaphore(0);
	static volatile Semaphore permitMessage = new Semaphore(1);
	
	static volatile Semaphore exitWaiting = new Semaphore(1);

	@BeforeClass
	public static void startServer(){
		threadServer.start();
	}
	
	public static void iniciarServerHilo(){
		Application.main(new String[]{ "--server.port=9000" });
		server.release(1);
	}
	
	/*
	@Test
	public void testConnection() throws Exception {
		
		WebSocketClient wsc = new WebSocketClient();
		wsc.connect("ws://127.0.0.1:9000/snake");
        wsc.disconnect();		
	}
	*/
	
	//Test 1
	@Test
	public void testAutomatico() throws Exception {
		System.out.println("-------------------------- inicioAutomático () ------------------------------");
		
		server.acquire(1);
		int num = 4;
		
		ScheduledExecutorService ex = Executors.newScheduledThreadPool(num);
        
		for (int i = 0; i< num; i++){
			if (i == 0){
				ex.execute(() -> testAutomatico("testAutomatico",true));
			}else{
				ex.execute(() -> testAutomatico("testAutomatico",false));
			}
		}		

		boolean trueEnding = finish.tryAcquire(4, 25, TimeUnit.SECONDS);
		
		Assert.assertTrue(trueEnding);
		
		//Thread.sleep(1500);		
	}
	
	private void testAutomatico(String room,boolean creator){
		try{
			WebSocketClient wsc = new WebSocketClient();
			
			wsc.connect("ws://127.0.0.1:9000/snake");
			
			wsc.onMessage((session, msg) -> {
				System.out.println("TestMessage: "+msg);

				if (msg.contains("created")){
					createdRoom.release(3);
				}
				
				if (msg.contains("join") && msg.contains("id")){
					finish.release();
				}
				
				if (msg.contains("joined")){
					permitMessage.release();
				}
			});			
			
			wsc.sendMessage("{\"type\":\"connect\",\"name\":\""+Thread.currentThread().getName()+"\"}");
			
			if (creator){
				wsc.sendMessage("{\"type\":\"create\",\"room\":\""+room+"\",\"tam\":\"4\"}");
			}
			else{
				createdRoom.acquire();
				
				permitMessage.acquire();
				
				wsc.sendMessage("{\"type\":\"joinroom\",\"room\":\""+room+"\"}");
			}						
		}catch(Exception e){
			e.printStackTrace();
		}
	}
		
	//Test 2
	@Test
	public void testManual() throws Exception{
		System.out.println("-------------------------- inicioManual() ------------------------------");
		server.acquire(1);
		int num = 2;
		
		ScheduledExecutorService ex = Executors.newScheduledThreadPool(num);
        
		for (int i = 0; i< num; i++){
			if (i == 0){
				ex.execute(() -> testManual("testManual", true));
			}else{
				ex.execute(() -> testManual("testManual", false));
			}
		}
		
		boolean trueEnding = finish.tryAcquire(2, 25, TimeUnit.SECONDS);

		Assert.assertTrue(trueEnding);
		
		Thread.sleep(1500);
	}
	
	private void testManual(String room, boolean creator){
		try{
			WebSocketClient wsc = new WebSocketClient();
			
			wsc.connect("ws://127.0.0.1:9000/snake");
			
			wsc.onMessage((session, msg) -> {
				System.out.println("TestMessage: "+msg);

				if (msg.contains("created")){
					createdRoom.release(1);
				}
				
				if (msg.contains("join") && msg.contains("id")){
					finish.release();
				}
				
				if (msg.contains("joined")){
					permitMessage.release();
				}
				
				if (msg.contains("canstart") && creator){
					try {
						wsc.sendMessage("{\"type\":\"start\",\"room\":\""+room+"\"}");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});			
			
			wsc.sendMessage("{\"type\":\"connect\",\"name\":\""+Thread.currentThread().getName()+"\"}");
			if (creator){
				wsc.sendMessage("{\"type\":\"create\",\"room\":\""+room+"\",\"tam\":\"4\"}");
			}
			else{
				createdRoom.acquire();
				
				permitMessage.acquire();
				wsc.sendMessage("{\"type\":\"joinroom\",\"room\":\""+room+"\"}");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//Test 3
	@Test
	public void testFinJuego() throws Exception{
		System.out.println("-------------------------- finJuego() ------------------------------");
		server.acquire(1);
		int num = 4;
		
		ScheduledExecutorService ex = Executors.newScheduledThreadPool(num);
        
		for (int i = 0; i< num; i++){
			if (i == 0){
				ex.execute(() -> testFinJuego("testFinJuego",true));
			}else{
				ex.execute(() -> testFinJuego("testFinJuego",false));
			}
		}

		boolean trueEnding = finish.tryAcquire(4, 25, TimeUnit.SECONDS);
		
		Assert.assertTrue(trueEnding);
		
		Thread.sleep(1500);
	}	
	
	private void testFinJuego(String room, boolean creator){
		try{
			WebSocketClient wsc = new WebSocketClient();
			
			wsc.connect("ws://127.0.0.1:9000/snake");
			
			wsc.onMessage((session, msg) -> {
				System.out.println("TestMessage: "+msg);

				if (msg.contains("created")){
					createdRoom.release(3);
				}
				
				if (msg.contains("joined")){
					permitMessage.release();
				}
				
				if (msg.contains("exit")){
					permitMessage.release();
					finish.release();
				}
			});
			
			
			wsc.sendMessage("{\"type\":\"connect\",\"name\":\""+Thread.currentThread().getName()+"\"}");
			
			if (creator){
				wsc.sendMessage("{\"type\":\"create\",\"room\":\""+room+"\",\"tam\":\"4\"}");
			}
			else{
				createdRoom.acquire();
				
				permitMessage.acquire();
				wsc.sendMessage("{\"type\":\"joinroom\",\"room\":\""+room+"\"}");

				permitMessage.acquire();
				wsc.sendMessage("{\"type\":\"exit\",\"room\":\""+room+"\"}");
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//Test 4
	@Test
	public void testEsperaEntrar() throws Exception{
		System.out.println("-------------------------- EsperaEntrar() ------------------------------");
		server.acquire(1);
		int num = 5;
		
		ScheduledExecutorService ex = Executors.newScheduledThreadPool(num);
        
		for (int i = 0; i< num; i++){
			if (i == 0){
				ex.execute(() -> testEsperaEntrar("testEsperaEntrar",true));
			}else{
				ex.execute(() -> testEsperaEntrar("testEsperaEntrar",false));
			}
		}

		boolean trueEnding = finish.tryAcquire(5, 25, TimeUnit.SECONDS);
		
		Assert.assertTrue(trueEnding);
		
		Thread.sleep(1500);
	}
	
	private void testEsperaEntrar(String room,boolean creator){
		try{
			WebSocketClient wsc = new WebSocketClient();
			wsc.connect("ws://127.0.0.1:9000/snake");
			
			wsc.onMessage((session, msg) -> {
				System.out.println("TestMessage: "+msg);

				if (msg.contains("created")){
					createdRoom.release(4);
					permitMessage.release();
					finish.release();
				}
				
				if (msg.contains("join") && msg.contains("id")){
					if (exitWaiting.tryAcquire()){
						try {
							permitMessage.acquire();
							wsc.sendMessage("{\"type\":\"exit\",\"room\":\""+room+"\"}");
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
				if (msg.contains("joined")){
					permitMessage.release();
					finish.release();
				}
				
				if (msg.contains("exit")){
					permitMessage.release();
				}
				
				if (msg.contains("wait")){
					permitMessage.release();
				}
			});
			
			permitMessage.acquire();
			wsc.sendMessage("{\"type\":\"connect\",\"name\":\""+Thread.currentThread().getName()+"\"}");
			permitMessage.release();
			
			if (creator){
				permitMessage.acquire();
				wsc.sendMessage("{\"type\":\"create\",\"room\":\""+room+"\",\"tam\":\"4\"}");
			}
			else{
				createdRoom.acquire();
				
				permitMessage.acquire();
				wsc.sendMessage("{\"type\":\"joinroom\",\"room\":\""+room+"\"}");
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	@Test
	public void test5() throws Exception{
		
		server.acquire(1);
		int num = 10;
		
		ScheduledExecutorService ex = Executors.newScheduledThreadPool(num);

		for (int i = 0; i< num; i++){
			if (i == 0)
				ex.execute(() -> test5("salaA",true));
			else if (i == 5)
				ex.execute(() -> test5("salaB",true));
			else if (i<5)
				ex.execute(() -> test5("salaA",false));
			else
				ex.execute(() -> test5("salaB",false));
		}
		

		boolean trueEnding = finish.tryAcquire(4, 60, TimeUnit.SECONDS);
		
		Assert.assertTrue(trueEnding);
		
		Thread.sleep(1500);
	}
	
	//No terminado
	private void test5(String room,boolean creator){
		try{
			WebSocketClient wsc = new WebSocketClient();
			
			wsc.connect("ws://127.0.0.1:9000/snake");
			
			wsc.onMessage((session, msg) -> {
				if (!msg.contains("update") && !msg.contains("cleansankes"))
				System.out.println("TestMessage: "+msg);

				if (msg.contains("created")){
					//createdRoom.release(3);
					//permitMessage.release();
				}
				
				if (msg.contains("join") && msg.contains("id")){
					//finish.release();
				}
				
				if (msg.contains("joined")){
					permitMessage.release();
				}
				
				if (msg.contains("exit")){
					//permitMessage.release();
				}
				
				if (msg.contains("update")){
					/*try {
						permitMessage.acquire();
						wsc.sendMessage("{\"type\":\"kill\"}");
						wsc.sendMessage("{\"type\":\"kill\"}");
						wsc.sendMessage("{\"type\":\"kill\"}");
						wsc.sendMessage("{\"type\":\"kill\"}");
						wsc.sendMessage("{\"type\":\"kill\"}");
						wsc.sendMessage("{\"type\":\"kill\"}");
						permitMessage.release();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
	/*
				}
				
				if (msg.contains("rank")){
					
					System.out.println("Raaaaaaaaaaaaaaaaaaaaaaaaaaaank:\n"+msg);
					permitMessage.release();
				}
			});
			
			//permitMessage.acquire();
			wsc.sendMessage("{\"type\":\"connect\",\"name\":\""+Thread.currentThread().getName()+"\"}");
			//permitMessage.release();
			
			//permitMessage.acquire();
			wsc.sendMessage("{\"type\":\"create\",\"room\":\""+Thread.currentThread().getName()+"\",\"tam\":\"4\"}");
			
			Thread.sleep(2000);
			
			//permitMessage.acquire();
			wsc.sendMessage("{\"type\":\"exit\",\"room\":\""+Thread.currentThread().getName()+"\"}");
			
			/*if (creator){
				//permitMessage.acquire();
				wsc.sendMessage("{\"type\":\"create\",\"room\":\""+room+"\",\"tam\":\"4\"}");
			}else{
				Thread.sleep(1000);
				//permitMessage.acquire();
				wsc.sendMessage("{\"type\":\"joinroom\",\"room\":\""+room+"\"}");
				
			}/
			
			Thread.sleep(10000);
			
			/*permitMessage.acquire();
			wsc.sendMessage("{\"type\":\"exit\",\"room\":\""+room+"\"}");/
				
			permitMessage.acquire();
			wsc.sendMessage("{\"type\":\"rank\"}");
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	*/
	

}
