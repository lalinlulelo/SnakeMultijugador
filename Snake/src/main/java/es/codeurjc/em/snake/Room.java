package es.codeurjc.em.snake;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Room {
	
	private final static long TICK_DELAY = 100;
	private final static long TICK_DELAY_FOOD = 6000;
	
	ConcurrentHashMap<Integer, Snake> snakesInRoom = new ConcurrentHashMap<Integer, Snake>();
	BlockingQueue<Snake> waiting = new ArrayBlockingQueue<Snake>(25);
	ConcurrentHashMap<Integer, Food> foodsInRoom = new ConcurrentHashMap<Integer, Food>();
	
	private AtomicInteger numSnakes = new AtomicInteger(0);
	private AtomicInteger numFoods = new AtomicInteger(0);
	private int maxSnakes;
	private final int maxFoods = 10;
	
	private ScheduledExecutorService scheduler;
	private ScheduledExecutorService scheduler2;		
	
	boolean started = false;
	
	private Snake creator = null;
	private SnakeGame game;
	
	Semaphore sem = new Semaphore (1);
	
	private boolean finish = false;
	
	public Room(int numMax, Snake creator, SnakeGame game){
		maxSnakes = numMax;
		this.creator = creator;
		addSnake(creator);
		this.game = game;
	}
	
	public Collection<Snake> getSnakes() {
		return snakesInRoom.values();
	}
	
	public Collection<Food> getFoods() {
		return foodsInRoom.values();
	}

	public int getMaxSnakes() {
		return maxSnakes;
	}

	public void setMaxSnakes(int maxSnakes) {
		this.maxSnakes = maxSnakes;
	}

	public Snake getCreator() {
		return creator;
	}

	public void setCreator(Snake creator) {
		this.creator = creator;
	}
	
	public void addFood() {
		Food f = new Food(numFoods.incrementAndGet());
		
		foodsInRoom.putIfAbsent(f.getId(), f);
		
		String food = String.format("{\"type\": \"food\",\"id2\": \""+f.getId()+"\",\"color\": \""
				+SnakeUtils.getRandomHexColor()+"\",\"x\": \""+f.getFLocation().x+"\",\"y\": \""+f.getFLocation().y+"\"}");
		
		try {
			broadcast(food);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void exitWaiting(Snake s){
		waiting.remove(s);
	}
	
	public boolean addSnake(Snake s){
		try {
			if (numSnakes.intValue() < maxSnakes){			
				
				Snake old = snakesInRoom.putIfAbsent(s.getId(), s);
				
				if (old != null){
					return false;
				}
				
				int count = numSnakes.getAndIncrement();
				
				if (numSnakes.intValue() > 1){
					creator.sendMessage(String.format("{\"type\": \"canstart\"}"));
				}
				
				if (count >= maxSnakes-1 && !started){
					startMatch();
					started = true;
				}else if (started && count <= maxSnakes){
					StringBuilder sb = new StringBuilder();	
					//sb = new StringBuilder();
					
					for (Snake snake : getSnakes()) {
						sb.append(String.format("{\"id\": %d, \"color\": \"%s\"}", snake.getId(), snake.getHexColor()));
						//sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\"}", snake.getId(), snake.getHexColor(), snake.getName()));
						sb.append(',');
					}
					sb.deleteCharAt(sb.length()-1);
					
					
					for (Food food : getFoods()) {
						String msgFood = String.format("{\"type\": \"food\",\"id\": \""+food.getId()+"\",\"color\": \""
								+food.getHexColor()+"\",\"x\": \""+food.getFLocation().x+"\",\"y\": \""+food.getFLocation().y+"\"}");
						s.sendMessage(msgFood);
					}
					
					String join = String.format("{\"type\": \"join\",\"data\":[%s]}", sb.toString());
					String joined = String.format("{\"type\": \"joined\"}");
					
					sb = new StringBuilder();
					sb.append(String.format("{\"id\": %d, \"color\": \"%s\"}", s.getId(), s.getHexColor()));
					//sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\"}", s.getId(), s.getHexColor(), s.getName()));
					
					String joinAll = String.format("{\"type\": \"join\",\"data\":[%s]}", sb.toString());
					s.sendMessage(joined);
					s.sendMessage(join);
					broadcast(joinAll);					
				}
					
			}else{
					if (started && numSnakes.get() <= maxSnakes){
						waiting.put(s);
						String wait = String.format("{\"type\": \"wait\"}");
						s.sendMessage(wait);
						return false;
					}
			}	
			
			updateSnakes();				
			return true;
			
		}catch (Exception e){
			e.printStackTrace();
		}
		return false;			
	}
	
	void updateSnakes(){
		try {
			for (Snake s : getSnakes()){
				String clean = String.format("{\"type\": \"cleansnakes\"}");
				s.sendMessage(clean);
				for (Snake ss : getSnakes()){
					String updateSnakes = String.format("{\"type\": \"snakes\",\"snake\":\""+ss.getName()+"\"}");
					s.sendMessage(updateSnakes);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean removeSnake(Snake s) {

		snakesInRoom.remove(Integer.valueOf(s.getId()));

		int count = numSnakes.decrementAndGet();
		
		if (count <= 1) {
			
			for (Snake ss : getSnakes()){
				try {
					//Se envia el leave de la que sale para que la ultima sepa que se va y borre los datos
					String leave = String.format("{\"type\": \"leave\", \"id\": %d}", s.getId());
					ss.sendMessage(leave);
					//Se envia el protocolo de salida para que el ultimo jugador salga de la partida
					String exit = String.format("{\"type\": \"exit\", \"id\": %d}", ss.getId());
					ss.sendMessage(exit);
					snakesInRoom.remove(Integer.valueOf(ss.getId()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				stopTimer();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}else{			
			if (!waiting.isEmpty()){
				try {
					Snake wait = waiting.poll();					
					
					addSnake(wait);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
		}
		
		return false;
	}
	
	public void removeFood(Food f){
		String eat = String.format("{\"type\": \"eat\", \"id\": \""+f.getId()+"\"}");
		
		try {
			broadcast(eat);
			foodsInRoom.remove(f.getId());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		foodsInRoom.remove(f.getId());
	}
	
	void startMatch() throws InterruptedException{
		StringBuilder sb = new StringBuilder();		
		//sb = new StringBuilder();
		addFood();
		
		for (Snake s : getSnakes()) {
			sb.append(String.format("{\"id\": %d, \"color\": \"%s\"}", s.getId(), s.getHexColor()));
			//sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\"}", s.getId(), s.getHexColor(), s.getName()));
			sb.append(',');
		}
		sb.deleteCharAt(sb.length()-1);
		
		String msg = String.format("{\"type\": \"join\",\"data\":[%s]}", sb.toString());
		
		try {			
			broadcast(msg);			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		started = true;
		startTimer();
		startFoodTimer();
	}
	
	private void tick(){		
		//sem.acquire();
		try {
			Snake mayor = null;
			for (Snake snake : getSnakes()) {
				snake.update(getSnakes());
				Food eat = snake.foodsCollisions(getFoods());
				
				if (eat != null){
					
					/*if (scores.contains(snake.getNombre())){
						scores.replace(snake.getNombre(), snake.getScore());
					}else{
						scores.putIfAbsent(snake.getNombre(), snake.getScore());
					}*/
					
					removeFood(eat);
				}
				
				if (mayor==null){
					mayor = snake;
				}else{
					if (mayor.getLength() < snake.getLength()){
						mayor = snake;
					}
				}
			}

			StringBuilder sb = new StringBuilder();
			//sb = new StringBuilder();
			for (Snake snake : getSnakes()) {
				sb.append(getLocationsJson(snake));
				sb.append(',');
			}
			sb.deleteCharAt(sb.length()-1);
			String msg = String.format("{\"type\": \"update\", \"data\" : [%s]}", sb.toString());

			broadcast(msg);
			
			if (numFoods.get() >= maxFoods && foodsInRoom.size() == 0 || mayor.getLength() > 10){
				Snake win = null;
				
				for (Snake s : getSnakes()){
					if (win == null){
						win = s;
					}else{
						if (s.getPuntuacion().intValue() > win.getPuntuacion().intValue()){
							win = s;
						}
					}
				}				
				mayor = null;
				String stop = String.format("{\"type\": \"win\", \"winner\" : \""+win.getName()+"\", \"score\" : \""+win.getPuntuacion().intValue()+"\"}");
				
				broadcast(stop);				
				stopTimer();
				stopFoodTimer();				
			}

		} catch (Throwable ex) {
			System.err.println("Exception processing tick()");
			ex.printStackTrace(System.err);
		}
		
		//sem.release();
	}
	
	private void tickFood() {
		try {			
			if (maxFoods > numFoods.get()){
				addFood();
			}else{
				stopFoodTimer();
			}			
		} catch (Throwable ex) {
			System.err.println("Exception processing tick()");
			ex.printStackTrace(System.err);
		}
	}
	
	private String getLocationsJson(Snake snake) {
		synchronized (snake) {
			StringBuilder sb = new StringBuilder();
			//sb = new StringBuilder();
			sb.append(String.format("{\"x\": %d, \"y\": %d}", snake.getHead().x, snake.getHead().y));
			for (Location location : snake.getTail()) {
				sb.append(",");
				sb.append(String.format("{\"x\": %d, \"y\": %d}", location.x, location.y));
			}

			return String.format("{\"id\":%d,\"body\":[%s]}", snake.getId(), sb.toString());
		}
	}
		
	/////////////////////
	////Coordenadas de la comida
	/*
	private String getLocationsJson2(Food food) {
		synchronized (food) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("{\"xFood\": %d, \"yFood\": %d}", food.getFLocation().x, food.getFLocation().y));
			

			return String.format("{\"room\": %d, \"id2\":%d, \"bodyFood\":[%s]}", food.getRoom(), food.getId(), sb.toString());
		}
	}
	*/
	//////////////////////	
		
	public void broadcast(String message) throws Exception {
		synchronized (this) {
			for (Snake snake : getSnakes()) {
				try {
					snake.sendMessage(message);

				} catch (Throwable ex) {
					System.err.println("Execption sending broadcast "+ message + " message to snake " + snake.getId());
					ex.printStackTrace(System.err);
					removeSnake(snake);
				}
			}
		}
		
	}
	
	public boolean finish(){
		return finish;
	}
	
	public void startTimer() {
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(() -> tick(), TICK_DELAY, TICK_DELAY, TimeUnit.MILLISECONDS);
		//scheduler.scheduleAtFixedRate(() -> tickFood(), TICK_DELAY_FOOD, TICK_DELAY_FOOD, TimeUnit.MILLISECONDS);
	}

	public void stopTimer() throws Exception {
		finish = true;
		for (Snake s : getSnakes()){
			game.putScore(s.getName(), s.getPuntuacion().intValue());
			s.resetGame();
		}
		numFoods.set(0);
		
		if (numSnakes.intValue() > 1){
			creator.sendMessage(String.format("{\"type\": \"canstart\"}"));
		}
		
		if (scheduler != null) {
			scheduler.shutdown();
		}
	}
	
	/////////////
	public void startFoodTimer() throws InterruptedException {
		scheduler2 = Executors.newScheduledThreadPool(1);
		scheduler2.scheduleAtFixedRate(() -> tickFood(), TICK_DELAY_FOOD, TICK_DELAY_FOOD, TimeUnit.MILLISECONDS);
	}

	public void stopFoodTimer() {
		System.out.println("Se acabaron las comidas");
		if (scheduler2 != null) {
			scheduler2.shutdown();
		}
	}
	////////////
	

}
