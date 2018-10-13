package es.codeurjc.em.snake;

public class Score {
	 private String nombre;
	 private int score;
	 
	 public Score(){
	  this.score= 0;
	 }
	 
	 public Score(String nombre, int score){
	  super();
	  this.nombre = nombre;
	  this.score = score;
	 }
	
	 public String getNombre() {
	  return nombre;
	 }
	
	 public void setNombre(String nombre) {
	  this.nombre = nombre;
	 }
	
	 public int getScore() {
	  return score;
	 }
	
	 public void setScore(int score) {
	  this.score = score;
	 }
	 
	 public void sumarScore(){
	  this.score ++;
	 }
}
