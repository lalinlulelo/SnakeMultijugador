var Console = {};

//variables auxiliares para no repetir nombres
var name;
var player_name_aux = [];
var player_name_bool;

var Id_nuevo;

// Evita que se inicie la comunicacion simultanea con varias salas
var permiso = 0;
var connection;

/*
//Comprueba si el nombre introducido es igual al array de todos los nombres creados
function comprobarNombre (argument){
	player_name_bool = 1;
	for(var a = 0; a < player_name_aux.length; a++){
		if(player_name_aux[a] === argument){
			player_name_bool = -1;
			return player_name_bool;
		}
	}
	return player_name_bool;
}


//  comprueba que sala que se recibe como argunento
function comprobarSala (argument){
	var valido = 1;
	for(var a = 0; a < rooms.length; a++){
		if(rooms[a] === argument){
			valido = -1;
			return valido;
		}
	}
	return valido;
}

function contarSala (){
	var cuenta = 0;
	for(var i = 0; i < players_room.length; i++){			
		if(players_room[i] !== -1){
				cuenta++;
		}
	}
    return cuenta;
}
*/

Console.log = (function(message) {
	var console = document.getElementById('console');
	var p = document.createElement('p');
	// crea el tablero de jugadores
	var players = document.getElementById('tablero');
	p.style.wordWrap = 'break-word';
	p.innerHTML = message;
	console.appendChild(p);
	while (console.childNodes.length > 25) {
		console.removeChild(console.firstChild);
	}
	console.scrollTop = console.scrollHeight;
});

let game;

class Snake {
	constructor() {
		this.snakeBody = [];
		this.color = null;
		this.name = 'none';	
		//this.room = null;
	}

	draw(context) {
		for (var pos of this.snakeBody) {
			context.fillStyle = this.color;
			context.fillRect(pos.x, pos.y,
				game.gridSize, game.gridSize);
		}
	}
}

//////////////
class Food {
	constructor(x, y) {
		this.posx = x;
		this.posy = y;
		this.color = null;
	}

	draw(context) {
		context.fillStyle = this.color;
		context.fillRect(this.posx, this.posy, game.gridSize, game.gridSize);
	}
}
///////////////

class Game {
	enviar(texto){
		this.socket.send(JSON.stringify(texto));
	}
	
	constructor(){
	
		this.fps = 30;
		this.socket = null;
		this.nextFrame = null;
		this.interval = null;
		this.direction = 'none';
		this.gridSize = 10;
		
		this.skipTicks = 1000 / this.fps;
		this.nextGameTick = (new Date).getTime();
	}

	initialize() {	
	
		this.snakes = [];		
		this.foods = [];
		
		// importa el tamaño de la ventana
		let canvas = document.getElementById('playground');
		if (!canvas.getContext) {
			Console.log('Error: 2d canvas not supported by this browser.');
			return;
		}
		// funcionamiento del teclado
		this.context = canvas.getContext('2d');
		window.addEventListener('keydown', e => {
			var code = e.keyCode;
			if (code > 36 && code < 41) {
				switch (code) {
				case 37:
					if (this.direction != 'east')
						this.setDirection('west');
					break;
				case 38:
					if (this.direction != 'south')
						this.setDirection('north');
					break;
				case 39:
					if (this.direction != 'west')
						this.setDirection('east');
					break;
				case 40:
					if (this.direction != 'north')
						this.setDirection('south');
					break;
				}
			}
		}, false);
		
		this.connect();
	}


	//crea un campo para registrar el nombre
	setName(){
		player_name = prompt("Please enter your name", "");
		
		//Meter nombres no creados previamente y que no sean vacios
		while ((player_name === null)||(player_name === "")||(comprobarNombre(player_name)===-1)){			
			alert ("Please insert a valid name");
			player_name = prompt("Please enter your name again", "");
		}
		
		player_name_aux.push(player_name);
		
		for (var i = 0; i < player_name_aux.length; i++){
			Console.log(player_name_aux[i] + " name created ");
		}
		
		name = "name " + player_name;
		player_name_bool = 0;
		this.socket.send(name);
		
	}

	
	/*
	// crea la sala
	setRoom(){
		// comprobamos y avisamos de las salas existentes
		for (var i = 0; i < rooms.length; i++){
			cadena = cadena + " " + rooms[i] + " ,";
		}
		n_room = prompt("|| Please enter your room code\n   " +
				"|| For create a room, insert a number \n   " +
				"|| In exception of the rooms which has been created before:\n   " 
				+ cadena + "\n || If you just want to enter in a room, just let the textfield empty");
		
		while ((!/^([0-9])*$/.test(n_room))||(comprobarSala(parseInt(n_room))===-1)){
			alert ("Please insert a correct number");
			n_room = prompt("Please enter your room code\n In exception of the rooms which has been created before:\n      " + cadena);
		}
		// en caso de que se desee entrar en una sala, comprobamos que el campo este vacío
		if ((n_room === "")||(n_room === null)){
				n_room = rooms[Math.floor(Math.random() * rooms.length)];
		}
		
		//Reseteamos la cadena
		cadena = "";
		dentro = 0;
	
		var sala = "room " + n_room;
		Console.log("enter to " + sala);
		this.socket.send(sala);
		
		var food = "food " + "";
		this.socket.send(food);
	}
	*/

	setDirection(direction) {			
		//if(inicio === 1){
			Console.log('Sent: Direction ' + direction);
			this.direction = direction;
			//this.socket.send(direction);
			this.socket.send('{"type":"move","direction":"'+direction+'"}');
		//}		
	}

	startGameLoop() {	
		this.nextFrame = () => {
			requestAnimationFrame(() => this.run());
		}
		
		this.nextFrame();		
	}

	stopGameLoop() {
		this.nextFrame = null;
		if (this.interval != null) {
			clearInterval(this.interval);
		}
	}

	draw() {
		this.context.clearRect(0, 0, 640, 480);
		for (var id in this.snakes) {			
			this.snakes[id].draw(this.context);
		}
		
		for (var id in this.foods){
			this.foods[id].draw(this.context);
		}
	}

	addSnake(id, color) {
		this.snakes[id] = new Snake();
		this.snakes[id].color = color;
		//this.snakes[id].name = name;
	}

	updateSnake(id, snakeBody) {
		if (this.snakes[id]) {
			this.snakes[id].snakeBody = snakeBody;
		}
	}

	removeSnake(id) {
		this.snakes[id] = null;
		// Force GC.
		delete this.snakes[id];
	}
	
	cleanSnakes(){
		this.snakes = [];
	}
	
	/////
	//Agregar o remover comida
	addFood(id2, color, x, y) {
		this.foods[id2] = new Food(x,y);
		this.foods[id2].color = color;
	}

	updateFood(id2, foodBody) {
		if (this.foods[id2]) {
			this.foods[id2].foodBody = foodBody;
		}
	}

	removeFood(id2){
		this.foods[id2] = null;
		delete this.foods[id2];
	}
	
	cleanFoods(){
		this.foods = [];
	}
	/////////

	run() {
	
		while ((new Date).getTime() > this.nextGameTick) {
			this.nextGameTick += this.skipTicks;
		}
		this.draw();
		if (this.nextFrame != null) {
			this.nextFrame();
		}
	}

	connect() {
		var ip = location.host;		
		this.socket = new WebSocket('ws://'+ip+'/snake');
		this.socket.onopen = () => {		
			
			var object = {
					type:"connect",
					name:document.getElementById("nombre").value
			};
			
			this.socket.send(JSON.stringify(object));
			
			// Socket open.. start the game loop.
			Console.log('Info: WebSocket connection opened.');
			// registramos el nomobre
			//this.setName();

			Console.log('Info: Press an arrow key to begin.');
			
			this.startGameLoop();
			
			//setInterval(() => this.socket.send('ping'), 5000);
		}

		this.socket.onclose = () => {
			Console.log('Info: WebSocket closed.');
			this.stopGameLoop();
		}
		
		this.unido = false;

		this.socket.onmessage = (message) => {

			var packet = JSON.parse(message.data);
			
			if (packet.type !== "update")
				Console.log("Type: "+packet.type);
			
			switch (packet.type) {				
				case 'conected':
					document.getElementById('div_nombre').style.display = 'none';
					document.getElementById('div_salas').style.display = 'block';
					
					var object = {
						type:"search"
					};

					this.socket.send(JSON.stringify(object));					
					
					break;
					
				case 'created':					
					document.getElementById('div_juego').style.display = 'block';
					document.getElementById('div_salas').style.display = 'none';
					document.getElementById("juegOcul").innerHTML= "Room created, waiting for other players";
					
					var object = {
						type:"snakes",
						room:document.getElementById("sala").value
					};
					this.socket.send(JSON.stringify(object));
					
					break;
					
				case 'canstart':
					document.getElementById("button_empezar").disabled = false;
					break;
					
				case 'joined':					
					document.getElementById('div_juego').style.display = 'block';
					document.getElementById('div_salas').style.display = 'none';
					document.getElementById("juegOcul").innerHTML= "Joined the room, waiting for others";
					
					var object = {
							type:"snakes",
							room:document.getElementById("sala").value
					};
					this.socket.send(JSON.stringify(object));
					this.unido = true;
					break;
					
				case 'notjoined':
					document.getElementById("salOcul").innerHTML= "Sala finalizando";
					break;
					
				case 'wait':				
					document.getElementById('div_juego').style.display = 'block';
					document.getElementById('div_salas').style.display = 'none';
					document.getElementById("juegOcul").innerHTML= "The room is full, waiting other players to enter";
					
					setTimeout(() => {
						var object = {
								type:"nowait",
								room:document.getElementById("sala").value
						};
						if (!this.unido){
							this.socket.send(JSON.stringify(object));
						}
					}, 5000);
					
					break;
					
				case 'nowait':				
					document.getElementById('div_juego').style.display = 'none';
					document.getElementById('div_salas').style.display = 'block';
					document.getElementById("juegOcul").innerHTML= "";
					
					break;
					
				case 'exit':
					document.getElementById('div_juego').style.display = 'none';
					document.getElementById('div_salas').style.display = 'block';
					
					this.cleanFoods();
					this.cleanSnakes();
					//this.removeSnake(packet.id);
					document.getElementById("btn_empezar").disabled = true;
					
					var object = {
						type:"search"
					};
					
					this.unido = false;
					this.socket.send(JSON.stringify(object));
					
					break;
					
				case 'clean':
					document.getElementById("div_listaSalas").innerHTML ="";
					
					break;
					
				case 'cleansnakes':					
					document.getElementById("div_listaJugadores").innerHTML ="";
					
					break;
					
				case 'search':					
					document.getElementById("div_listaSalas").innerHTML += packet.room+"<br />";
					
					break;
				
				case 'snakes':				
					document.getElementById("div_listaJugadores").innerHTML += packet.snake+"<br />";
					
					break;
				
				case 'clearrank':
					document.getElementById("div_listaPuntuaciones").innerHTML = "";
					break;
				
				case 'rank':
					document.getElementById("div_listaPuntuaciones").innerHTML += packet.name+", puntuaction "+packet.score+"<br />";
					break;
					
				case 'update':					
					for (var i = 0; i < packet.data.length; i++) {
						this.updateSnake(packet.data[i].id, packet.data[i].body);
					}
					break;
					
				case 'join':
					Console.log("Room where start join " + sala.value);
					document.getElementById("juegOcul").innerHTML= "";
					for (var j = 0; j < packet.data.length; j++) {
						//this.addSnake(packet.data[j].id, packet.data[j].color, packet.data[j].name);
						this.addSnake(packet.data[j].id, packet.data[j].color);
					}
					break;
					
				case 'food':
					this.addFood(packet.id2, packet.color, packet.x, packet.y);
					var idFood = packet.id2;
					Console.log('Food with id ' + idFood);
					break;
					
				case 'eat':
					this.removeFood(packet.id);					
					break;
					
				case 'snakeEat':
					name = packet.name;							
					var length_jugador = packet.longitud;		
					var puntuacion_actual = packet.puntuacion;
					
					Console.log(name + ": eat a food");
					Console.log(name + " length: " + length_jugador);
					Console.log(name + " points: " + puntuacion_actual);
					break;
					
				case 'leave':
					this.removeSnake(packet.id);
					break;
					
				case 'dead':
					name = packet.name;							
					var length_jugador = packet.longitud;		
					var puntuacion_actual = packet.puntuacion;
					
					Console.log(name + ": Your snake is dead, bad luck!");
					Console.log(name + " length: " + length_jugador);
					Console.log(name + " points: " + puntuacion_actual);
					
					this.direction = 'none';
					break;
					
				case 'kill':
					name = packet.name;
					var length_jugador = packet.longitud;
					var puntuacion_actual = packet.puntuacion; 
						
					Console.log(name + ": Head shot!");
					Console.log(name + " length: " + length_jugador);
					Console.log(name + " points: " + puntuacion_actual);					
					break;
					
				case 'win':
					alert("The winner is: "+packet.winner+", whose score is: "+packet.score);
					this.cleanSnakes();
					
					this.cleanFoods();
					//btn_empezar.disabled= false;
					break;										
			}
		}
		
		connection = this.socket;
	}
}

var button_nombre = document.getElementById('button_nombre');
button_nombre.onclick = function(){
	var name = document.getElementById('nombre');
	
	if(name.value !== ""){
		game = new Game();
		game.initialize();
	}else{
		document.getElementById("nombOcul").innerHTML= "You didn't write your name!";
	}
};

var sala = document.getElementById('sala');
var tama = document.getElementById('tam');
var btn_crearSala = document.getElementById('button_crearSala');
btn_crearSala.onclick = function(){
	if(sala.value !== "" && tama.value === "2" || tama.value === "3" || tama.value === "4"){
		
		var object = {
			type:"create",
			room:document.getElementById("sala").value,
			tam:document.getElementById("tam").value
		};
		console.log(JSON.stringify(object));
		connection.send(JSON.stringify(object));
	}else{
		document.getElementById("salOcul").innerHTML= "The room name is empty or the number of players is not supported";
	}
};

var btn_actualizar = document.getElementById('button_actualizar');
btn_actualizar.onclick = function(){
	
	var object = {
			type:"search"
		};
					
		connection.send(JSON.stringify(object));
}

var btn_unirSala = document.getElementById('button_unirSala');
btn_unirSala.onclick = function(){
	if(sala.value === ""){
		document.getElementById("salOcul").innerHTML= "Room name is missing";
		
	}else if(true/*sala no existe*/){
		document.getElementById("salOcul").innerHTML= "The room is full or it doesn't exist";
		var object = {
				type:"joinroom",
				room:sala.value
		};
						
			connection.send(JSON.stringify(object));
	}else{
		
	}
};

var btn_empezar = document.getElementById('button_empezar');
btn_empezar.onclick = function(){
	//if (this.unido === true){
	var object = {
			type:"start",
			room:sala.value
	};				
	connection.send(JSON.stringify(object));	
	//}
	this.disabled = true;
}

var btn_puntuaciones = document.getElementById('button_puntuaciones');
btn_puntuaciones.onclick = function(){
	document.getElementById('div_salas').style.display = 'none';
	document.getElementById('div_puntuaciones').style.display = 'block';
	
	var object = {
		type:"rank"
	};
				
	connection.send(JSON.stringify(object));
};

var btn_regresar = document.getElementById('button_regresar');
btn_regresar.onclick = function(){
	
	document.getElementById('div_puntuaciones').style.display = 'none';	
	document.getElementById('div_salas').style.display = 'block';
};

var btn_cerrar = document.getElementById('button_cerrar');
btn_cerrar.onclick = function(){
	
	var object = {
			type:"exit",
			room:sala.value
		};
	connection.send(JSON.stringify(object));
};
