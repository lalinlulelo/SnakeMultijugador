package es.codeurjc.em.snake;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class Snake {

	private static final int DEFAULT_LENGTH = 5;

	private final int id;
	//variable que contendrá el nombre del snake
	private String name;

	// atributo sala
	private Room room;
	
	private Location head;
	private final Deque<Location> tail = new ArrayDeque<>();
	private int length = DEFAULT_LENGTH;
	
	private AtomicInteger puntuacion = new AtomicInteger(0);

	private final String hexColor;
	private Direction direction;

	private final WebSocketSession session;
	
	// Comida auxiliar para guardar en caso de ser comido
	private Food foodAux;

	public Snake(int id, WebSocketSession session) {
		this.id = id;
		this.session = session;
		this.hexColor = SnakeUtils.getRandomHexColor();
		resetState();
	}

	private void resetState() {
		this.direction = Direction.NONE;
		this.head = SnakeUtils.getRandomLocation();
		this.tail.clear();
		
		this.puntuacion.decrementAndGet();
		
		this.length = DEFAULT_LENGTH;
	}
	
	
	//muere solo en su sala
	private synchronized void kill() throws Exception {
		resetState();				
		
		String msg = String.format("{\"type\": \"dead\", \"id\": %d, \"color\": \"%s\", \"name\": \"%s\", \"longitud\": %d, \"puntuacion\": %d}", 
									this.getId(), this.getHexColor(), this.getName(), this.getLength(), this.getPuntuacion().intValue());	
			
		//String msg = String.format("{\"type\": \"dead\", \"id\": %d, \"color\": \"%s\", \"name\": \"%s\", \"longitud\": %d}", 
		//		this.getId(), this.getHexColor(), this.getName(), this.getLength());	
		sendMessage(msg);
		//sendMessage("{\"type\": \"dead\"}");
	}
	
	//matara solo a los de su sala
	private synchronized void reward() throws Exception {
		//Por cada muerte provocada longitud y puntuacion +1
		this.length++;
		this.puntuacion.incrementAndGet();
				
		//Mensaje de asesinato
		String msg = String.format("{\"type\": \"kill\", \"id\": %d, \"color\": \"%s\", \"name\": \"%s\", \"longitud\": %d, \"puntuacion\": %d}",
									this.getId(), this.getHexColor(), this.getName(), this.getLength(), this.getPuntuacion().intValue());

		//String msg = String.format("{\"type\": \"kill\", \"id\": %d, \"color\": \"%s\", \"name\": \"%s\", \"longitud\": %d}", 
		//		this.getId(), this.getHexColor(), this.getName(), this.getLength());
		sendMessage(msg);
		
		//sendMessage("{\"type\": \"kill\"}");
	}
	
	/*
	private synchronized void maxLongitud() throws Exception {		
		///Finaliza la partida cuando solo queda un jugador por abandono de otros, 
		//por tamaño de serpientes o bien porque hayan aparecido todas las comidas posibles
		
			// notifica por consola que el jugador que ha alcanzado su longitud maxima
		
			//Fin de la partida en la habitacion recibida
			String msg = String.format("{\"type\": \"finLongitud\", \"room\": %d,"
					+ " \"id\": %d, \"color\": \"%s\", \"name\": \"%s\", \"longitud\": %d, \"puntuacion\": %d}",
					this.getRoom(), this.getId(), this.getHexColor(), this.getName(), this.getLength(), this.getPuntuacion());
						
			sendMessage(msg);
	}
	*/	
	
	public void resetGame(){
		this.direction = Direction.NONE;
		this.head = SnakeUtils.getRandomLocation();
		this.tail.clear();
		this.length = DEFAULT_LENGTH;
		this.puntuacion.set(0);
	}
	
	private synchronized void eat() throws Exception {
		this.length++;
		this.puntuacion.incrementAndGet();
		
		String msg = String.format("{\"type\": \"snakeEat\", \"color\": \"%s\", \"name\": \"%s\", \"longitud\": %d, \"puntuacion\": %d}",
				 this.getHexColor(), this.getName(), this.getLength(), this.getPuntuacion().intValue());

		sendMessage(msg);
		/*
			String msg = String.format("{\"type\": \"eat\", \"room\": %d,"
					+ " \"id\": %d, \"color\": \"%s\", \"name\": \"%s\", \"longitud\": %d, \"puntuacion\": %d,"
					+ " \"id2\": %d, \"color2\": \"%s\"}",
					this.getRoom(), this.getId(), this.getHexColor(), this.getName(), this.getLength(), this.getPuntuacion()
					, foodAux.getId(), foodAux.getHexColor());		
			
			sendMessage(msg);
		*/	
			/*
			for (Food food : snakeGame.getFoods()) {
				System.out.println("Comida detectada es " + food.getId());
				if(food == foodAux){
					System.out.println("Comida reseteada es " + food.getId());
					food.resetState();	
				}					
			}
			*/
			
	}	

	protected void sendMessage(String msg) throws Exception {
		this.session.sendMessage(new TextMessage(msg));
	}

	public synchronized void update(Collection<Snake> snakes) throws Exception {

		Location nextLocation = this.head.getAdjacentLocation(this.direction);

		if (nextLocation.x >= Location.PLAYFIELD_WIDTH) {
			nextLocation.x = 0;
		}
		if (nextLocation.y >= Location.PLAYFIELD_HEIGHT) {
			nextLocation.y = 0;
		}
		if (nextLocation.x < 0) {
			nextLocation.x = Location.PLAYFIELD_WIDTH;
		}
		if (nextLocation.y < 0) {
			nextLocation.y = Location.PLAYFIELD_HEIGHT;
		}

		if (this.direction != Direction.NONE) {
			this.tail.addFirst(this.head);
			if (this.tail.size() > this.length) {
				this.tail.removeLast();
			}
			this.head = nextLocation;
		}

		handleCollisions(snakes);
	}
	
	public synchronized Food foodsCollisions(Collection<Food> foods) throws Exception {
		//Food ret = null;
		foodAux = null;
		for (Food food : foods){		
			if (this.getHead().equals(food.getFLocation())){
				System.out.println("Colision con comida con Id " + food.getId());
				foodAux = food;
				eat();
				//ret = food;				
				break;
			}
		}
		return foodAux;
	}

	private void handleCollisions(Collection<Snake> snakes) throws Exception {

		for (Snake snake : snakes) {

			boolean headCollision = this.id != snake.id && snake.getHead().equals(this.head);
			boolean tailCollision = snake.getTail().contains(this.head);
			
			/*
			//Condicion de choque con la comida
			boolean foodCollision = false;
			//Recorre toda la lista de comidas que hay en el snakeGame creado en SnakeHandler
			for (Food food : snakeGame.getFoods()) {
				if(food.getRoom() == this.getRoom ()){
					foodCollision = snake.getHead().equals(food.getFLocation());	
					if(foodCollision == true){
						System.out.println("Comida con auxiliar por colision " + food.getId());
						foodAux = food;
						break;
					}
				}				
			}
			*/
			
			// realizamos un boolean, para que se maten entre los mismos de una sala
			//boolean isRoom = this.room == snake.room;

			if ((headCollision || tailCollision)&&(snake.getLength()!=1)) {
				kill();
				if (this.id != snake.id) {
					snake.reward();				
				}
			}
			
			/*
			if (foodCollision) {
				System.out.println("Ha habido colision con comida.");
				snake.eat();			
			}
			*/
			
			//snake.maxLongitud();
		}
	}

	public synchronized Location getHead() {
		return this.head;
	}

	public synchronized Collection<Location> getTail() {
		return this.tail;
	}

	public synchronized void setDirection(Direction direction) {
		this.direction = direction;
	}

	public int getId() {
		return this.id;
	}

	public String getHexColor() {
		return this.hexColor;
	}
	
	//devuelve el nombre del snake
	public String getName() {
		return this.name;
	}
	// coloca nuevo valor al snake
	public synchronized void setName (String name) {
		this.name = name;
	}
	
	// devuelve el nombre del sala
	public Room getRoom() {
		return this.room;
	}
	//coloca nuevo valor al sanke
	public synchronized void setRoom (Room room) {
		this.room = room;
	}
	
	///////////////////
	public int getLength() {
		return this.length;
	}
	
	public synchronized void setLength (int length) {
		this.length = length;
	}
	
	public AtomicInteger getPuntuacion() {
		return this.puntuacion;
	}

	public void setPuntuacion(AtomicInteger puntuacion) {
		this.puntuacion = puntuacion;
	}
	///////////////////
}
