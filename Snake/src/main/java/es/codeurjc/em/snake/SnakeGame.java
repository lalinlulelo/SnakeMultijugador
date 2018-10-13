package es.codeurjc.em.snake;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SnakeGame {	
	private ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
	//ConcurrentHashMap<String, AtomicInteger> scores = new ConcurrentHashMap<String, AtomicInteger>();
	ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<String, Integer>();
	
	public void start(String room)throws Exception{
		rooms.get(room).startMatch();
	}
	
	public boolean addRoom(String room, int max, Snake creator){
		Room old = rooms.putIfAbsent(room, new Room(max, creator, this));
		
		if (old != null){
			return false;
		}
		
		return true;
	}
	
	public ConcurrentHashMap<String, Room> getRooms() {
		return rooms;
	}
	
	public boolean addSnake(String room, Snake snake) {
		return rooms.get(room).addSnake(snake);
	}
	
	public void exitWaiting(String room, Snake s){
		if (rooms.contains(room))
			rooms.get(room).exitWaiting(s);
	}
	
	public synchronized boolean removeSnake(String room, Snake snake) {
		boolean empty = rooms.get(room).removeSnake(snake);
		
		if (empty){
			rooms.remove(room);
			return true;
		}
		
		return false;
	}
	
	public void broadcast(String room, String message){
		try {
			rooms.get(room).broadcast(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	public List<Score> getScores(){
		ArrayList<Score> list = new ArrayList<Score>();
		
		//for (Entry<String, AtomicInteger> e : scores.entrySet()){
		for (Entry<String, Integer> e : scores.entrySet()){
			list.add(new Score(e.getKey(),e.getValue().intValue()));
		}
		
		Collections.sort(list, new Comparator<Object>() {
			@Override
			public int compare(Object p1, Object p2) {
				if(p1 instanceof Score){
					Score puntuacion1 = (Score)p1;
					Score puntuacion2 = (Score)p2;
					
					return new Integer(puntuacion2.getScore()).compareTo(new Integer(puntuacion1.getScore()));
				}else{
					return 0;
				}
			}
		});
		
		if (list.size() < 9){
			return (List<Score>)list.subList(0, list.size());
		}else{
			return (List<Score>)list.subList(0, 9);
		}
	}
	
	/*
	public void putScore(String key, AtomicInteger value){
		if (scores.containsKey(key)){
			if (scores.get(key).get()<value.get()){
				scores.put(key, value);
			}
		}else{
			scores.put(key, value);
		}
	}
	*/
	public void putScore(String key, int value){
		if (scores.containsKey(key)){
			if (scores.get(key)<value){
				scores.put(key, value);
			}
		}else{
			scores.put(key, value);
		}
	}
}
