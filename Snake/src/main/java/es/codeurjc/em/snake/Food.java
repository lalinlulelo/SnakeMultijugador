package es.codeurjc.em.snake;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class Food {
	private final int id2;
	private Location flocation;
	private String hexColor2;
	
	private int length = 1;
	
	//atributo sala
	private int room;
	private WebSocketSession session;

	public Food(int id) {
		this.id2 = id;
		this.flocation = SnakeUtils.getRandomLocation();
		this.hexColor2 = SnakeUtils.getRandomHexColor();
				
		resetState();
	}

	public void resetState() {
			this.flocation = SnakeUtils.getRandomLocation();
			this.hexColor2 = SnakeUtils.getRandomHexColor();
	}
	
	protected void sendMessage(String msg) throws Exception {
		this.session.sendMessage(new TextMessage(msg));
	}
	

	public synchronized Location getFLocation() {
		return this.flocation;
	}

	public int getId() {
		return this.id2;
	}

	public String getHexColor() {
		return this.hexColor2;
	}

	// devuelve el nombre del sala
	public int getRoom() {
		return this.room;
	}
	//  coloca nuevo valor al sanke
	public synchronized void setRoom (int room) {
		this.room = room;
	}
	
	public int getLength() {
		return this.length;
	}
	
	public synchronized void setLength (int length) {
		this.length = length;
	}
	
}
