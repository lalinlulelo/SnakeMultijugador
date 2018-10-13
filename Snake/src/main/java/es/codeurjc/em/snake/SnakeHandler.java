package es.codeurjc.em.snake;
// [1] se importa la colección lista
import java.util.List;
import java.util.Map.Entry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.qos.logback.core.net.SyslogOutputStream;

public class SnakeHandler extends TextWebSocketHandler {
	
	private ObjectMapper mapper = new ObjectMapper();
	private ConcurrentHashMap<String, WebSocketSession> conexiones = new ConcurrentHashMap<>();

	private static final String SNAKE_ATT = "snake";
	private static final String FOOD_ATT = "food";
	
	private AtomicInteger snakeIds = new AtomicInteger(0);
	private AtomicInteger foodIds = new AtomicInteger(0);
	
	public static SnakeGame snakeGame = new SnakeGame();
	
	//StringBuilder sb =  new StringBuilder ();
	
	/*
	// arraylist sincronizado que contendrá el conjunto de mensajes con el nombre
	List<String> menssages_players = Collections.synchronizedList(new ArrayList<String>());
	// arraylist sincronizado que contendrá el conjuto de mensajes con el join
	List<String> messages_join = Collections.synchronizedList(new ArrayList<String>());	
	//arraylist sincronizado que contendrá el conjuto de mensajes con food
	List<String> messages_food = Collections.synchronizedList(new ArrayList<String>());
	*/
	
	//arrylist sincronizado que contendrá el cojunto de salas
	List<Room> rooms = Collections.synchronizedList(new ArrayList<Room>());
	
//////////////////////////////////////////////////
	//Métodos    	
	/*
	private void mensajero (String tipo, Snake s, Room room) throws Exception{
		String msg;
		
		gameSemaphore.acquire();
		
		switch (tipo){
		
			case "join":
				// recorre la lista de serpientes escogiendo aquellas que se encuentren en la misma sala
				for (Snake snake : snakeGame.getSnakes()) {
					if(snake.getRoom() == s.getRoom ()){
						sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\","
								+ "\"snake_1\": \"%s\", \"snake_2\": \"%s\", \"snake_3\": \"%s\", \"snake_4\": \"%s\" }",
								snake.getId(), snake.getHexColor(), snake.getName (), room.snakes[0], room.snakes[1], room.snakes[2], room.snakes[3]));
						sb.append(',');	
					}								
				}
				sb.deleteCharAt(sb.length()-1);
				
				msg = String.format("{\"type\": \"join\", \"room\": %d, \"data\": [%s]}", room.nombre, sb.toString());
				System.out.println(msg);
				messages_join.add(msg);
				
				//se envía cada mensaje almacenado en la lista
				for(int i = 0; i < messages_join.size(); i++){
					snakeGame.broadcast(messages_join.get(i));
				}
				sb.setLength(0);
				break;
				
			case "player":
				sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\","
						+ " \"snake_1\": \"%s\", \"snake_2\": \"%s\", \"snake_3\": \"%s\", \"snake_4\": \"%s\" , \"puntuacion\": %d}",
						s.getId(), s.getHexColor(), s.getName(), room.snakes[0], room.snakes[1], room.snakes[2], room.snakes[3], s.getPuntuacion()));
				
				msg = String.format("{\"type\": \"player\", \"room\": %d, \"data\": [%s]}", room.nombre, sb.toString());				
				System.out.println(msg);
				menssages_players.add(msg);
				//se envia cada mensaje almacenado en la lista
				for(int i = 0; i < menssages_players.size(); i++){
					snakeGame.broadcast(menssages_players.get(i));
				}
				sb.setLength(0);
				break;
				
			case "wait_queue":
				int index = buscarIdRoomNombre(room.nombre);				
				StringBuilder sb_wait = new StringBuilder();
				sb_wait.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\", \"personas\": \"%d\"}", s.getId(), s.getHexColor(), s.getName(), room.n_personas));
				msg = String.format("{\"type\": \"wait_queue\", \"data\": [%s]}", sb_wait.toString());
				System.out.println(msg);
				snakeGame.broadcast(msg);
				sb.setLength(0);
				break;
				
			case "count":
				sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\"}", s.getId(), s.getHexColor(), s.getName()));
				sb.append(',');
				sb.deleteCharAt(sb.length()-1);
				msg = String.format("{\"type\": \"count\", \"data\": [%s]}", sb.toString());
				System.out.println(msg);
				snakeGame.broadcast(msg);
				sb.setLength(0);
				break;
				
			case "wait_queue_expired":
				sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\"}", s.getId(), s.getHexColor(), s.getName()));
				sb.append(',');
				sb.deleteCharAt(sb.length()-1);
				msg = String.format("{\"type\": \"wait_queue_expired\", \"data\": [%s]}", sb.toString());
				System.out.println(msg);
				snakeGame.broadcast(msg);
				sb.setLength(0);
				break;
				
			case "room_update":
				if(rooms.size() > 0){
					for(int i = 0; i < rooms.size(); i++){
							sb.append(String.format("{\"room_number\": %d, \"name\": \"%s\"}", rooms.get(i).nombre, s.getName()));
							sb.append(',');
					}
					sb.deleteCharAt(sb.length()-1);
					
					msg = String.format("{\"type\": \"room_update\", \"data\": [%s]}", sb.toString());
					System.out.println(msg);
					snakeGame.broadcast(msg);
				}else{
					sb.append(String.format("{\"room_number\": %d}", -1));
					msg = String.format("{\"type\": \"room_update\", \"data\": [%s]}", sb.toString());
					snakeGame.broadcast(msg);
				}
				sb.setLength(0);
				break;
			
			case "player_leave":								
				sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\","
						+ " \"snake_1\": \"%s\", \"snake_2\": \"%s\", \"snake_3\": \"%s\", \"snake_4\": \"%s\" , \"puntuacion\": %d}",
						s.getId(), s.getHexColor(), s.getName(), room.snakes[0], room.snakes[1], room.snakes[2], room.snakes[3], s.getPuntuacion()));

				// añadimos en el mensaje la habitación
				msg = String.format("{\"type\": \"player_leave\", \"room\": %d, \"data\": [%s]}", room.nombre, sb.toString());	
				System.out.println(msg);
				snakeGame.broadcast(msg);
				sb.setLength(0);
				break;
				
			case "leave":
				msg = String.format("{\"type\": \"leave\", \"room\": %d, \"id\": %d}", room.nombre, s.getId());		
				System.out.println(msg);
				snakeGame.broadcast(msg);
				sb.setLength(0);
				break;
				
			case "inicio":				
				msg = String.format("{\"type\": \"inicio\", \"room\": %d}", room.nombre);	
				System.out.println(msg);
				snakeGame.broadcast(msg);
				sb.setLength(0);
				break;
			
				
			case "food":
				//recorre la lista de serpientes escogiendo aquellas que se encuentren en la misma sala
				for (Food food : snakeGame.getFoods()) {
					if(food.getRoom() == s.getRoom()){
						//sb.append(String.format("{\"id2\": %d, \"color2\": \"%s\", \"room\": %d}", food.getId(), food.getHexColor(), food.getRoom()));
						sb.append(String.format("{\"id2\": %d, \"color2\": \"%s\"}", food.getId(), food.getHexColor()));
						sb.append(',');	
					}								
				}
				sb.deleteCharAt(sb.length()-1);
				
				msg = String.format("{\"type\": \"food\", \"room\": %d, \"data\": [%s]}", room.nombre, sb.toString());
				System.out.println(msg);
				messages_food.add(msg);
				//se envía cada mensaje almacenado en la lista
				for(int i = 0; i < messages_food.size(); i++){
					snakeGame.broadcast(messages_food.get(i));
				}
				sb.setLength(0);
				break;
				
			case "showFinal":
				//recorre la lista de serpientes escogiendo aquellas que se encuentren en la misma sala
				for (Snake snake : snakeGame.getSnakes()) {
					if(snake.getRoom() == s.getRoom ()){
						sb.append(String.format("{\"id\": %d, \"color\": \"%s\", \"name\": \"%s\","
								+ " \"snake_1\": \"%s\", \"snake_2\": \"%s\", \"snake_3\": \"%s\", \"snake_4\": \"%s\" , \"puntuacion\": %d}",
								s.getId(), s.getHexColor(), s.getName(), room.snakes[0], room.snakes[1], room.snakes[2], room.snakes[3], s.getPuntuacion()));
						sb.append(',');	
					}								
				}
				sb.deleteCharAt(sb.length()-1);
				msg = String.format("{\"type\": \"showFinal\", \"room\": %d, \"data\": [%s]}", room.nombre, sb.toString());
				
				snakeGame.broadcast(msg);
				sb.setLength(0);

				break;				
		}		
		gameSemaphore.release();	
	}
	*/
	
	//metodo que envia la informacion en caso de establecer una conexion y enviar join
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {								
		if (!conexiones.containsKey(session.getId())){			
			int id = snakeIds.getAndIncrement();
			Snake s = new Snake(id, session);			
			session.getAttributes().put(SNAKE_ATT, s);

			conexiones.put(session.getId(), session);			
			
			session.sendMessage(transformMessage("type", "testconected"));
		}		
	}
	
	// Metodo que se emplea al recibir un mensaje del cliente
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {		
		try {			
			JsonNode node = mapper.readTree(message.getPayload().toString());
			String type = node.get("type").textValue();
			
			System.out.println(message.getPayload().toString());

			//String payload = message.getPayload();
			Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);			
			
			switch (type){
				case "ping":{
					return;
				}
								
				case "connect":{
					s.setName(node.get("name").textValue());
					session.sendMessage(transformMessage("type", "conected"));
					break;
				}
				
				case "create":{
					String room = node.get("room").textValue();

					boolean createdRoom = snakeGame.addRoom(room, node.get("tam").asInt(), s);
					
					if (createdRoom){
						session.sendMessage(transformMessage("type", "created"));
					}else{
						session.sendMessage(transformMessage("type", "notcreated"));
					}
					break;
				}
				
				case "joinroom":{
					String room = node.get("room").textValue();
					
					if (snakeGame.getRooms().containsKey(room)){
						if (!snakeGame.getRooms().get(room).finish()){
							boolean join = snakeGame.addSnake(room, s);
							
							if (join){
								session.sendMessage(transformMessage("type", "joined"));
							}
							
						}else{
							session.sendMessage(transformMessage("type", "notjoined"));
						}
					}					
					break;
				}
				
				case "search":{
					session.sendMessage(transformMessage("type", "clean"));
					
					for (Entry<String, Room> e : snakeGame.getRooms().entrySet()){
						session.sendMessage(transformMessage("type", "search","room", e.getKey().toString()));
					}
					break;
				}
				
				case "snakes":{
					session.sendMessage(transformMessage("type", "cleansnakes"));
					
					for (Snake snak : snakeGame.getRooms().get(node.get("room").textValue()).getSnakes()){
						session.sendMessage(transformMessage("type", "snakes","snake", snak.getName()));
					}
					break;
				}
				
				case "move":{
					String direction = node.get("direction").textValue();
					Direction d = Direction.valueOf(direction.toUpperCase());
					s.setDirection(d);
					break;
				}
				
				case "nowait":{
					snakeGame.exitWaiting(node.get("room").textValue(), s);
					session.sendMessage(transformMessage("type", "nowait"));
					break;
				}
				
				case "start":{
					System.out.println(node.get("room").textValue());
					snakeGame.start(node.get("room").textValue());
					break;
				}
				
				case "rank":{
					session.sendMessage(transformMessage("type", "clearrank"));
					List<Score> score = snakeGame.getScores();
					
					for (Score ss : score){	
						session.sendMessage(transformMessage("type", "rank","name",ss.getNombre(),"score",Integer.toString(ss.getScore())));
					}					
					break;
				}
				
				case "exit":{
					session.sendMessage(transformMessage("type", "exit"));
					snakeGame.removeSnake(node.get("room").textValue(), s);					
				}				
			}

		} catch (Exception e) {
			System.err.println("Exception processing message " + message.getPayload());
			e.printStackTrace(System.err);
		}
		
	}
	
	// si se cierra la conexión...
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {	
		if (conexiones.containsKey(session.getId())){
			System.out.println("Connection closed. Session " + session.getId());		
			Snake s = (Snake) session.getAttributes().get(SNAKE_ATT);			
			String room = null;
			
			for (Entry<String, Room> e : snakeGame.getRooms().entrySet()){
				if (e.getValue().snakesInRoom.containsValue(s)){
					room = e.getKey();
					break;
				}
			}			
			
			if (room != null && !snakeGame.removeSnake(room, s)){
				String leave = String.format("{\"type\": \"leave\", \"id\": %d}", s.getId());				
				snakeGame.broadcast(room, leave);
			}
			
			conexiones.remove(session.getId());
		}	
	}	
	
	public TextMessage transformMessage(String action, String text){
		ObjectNode responseNode = mapper.createObjectNode();
		responseNode.put(action, text);
		
		return new TextMessage(responseNode.toString());
	}
	
	public TextMessage transformMessage(String action, String text,String action2, String text2){
		ObjectNode responseNode = mapper.createObjectNode();
		responseNode.put(action, text);
		responseNode.put(action2, text2);
		
		return new TextMessage(responseNode.toString());
	}
	
	public TextMessage transformMessage(String action, String text, String action2, String text2, String action3, String text3){
		ObjectNode responseNode = mapper.createObjectNode();
		responseNode.put(action, text);
		responseNode.put(action2, text2);
		responseNode.put(action3, text3);
		return new TextMessage(responseNode.toString());
	}
}
