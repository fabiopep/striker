package com.mygdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.Striker;
import com.mygdx.game.scenes.DirHud;
import com.mygdx.game.scenes.Hud;
import com.mygdx.game.scenes.StrHud;
import com.mygdx.game.sprites.Ball;
import com.mygdx.game.sprites.Pin;
import com.mygdx.game.tools.B2WorldCreator;

import com.mygdx.game.tools.WorldContactListener;

/**
 * Created by User on 07/07/2016.
 */
public class PlayScreen implements Screen {
    //Reference to our Game, used to set Screens
    private Striker game;
    public static boolean alreadyDestroyed = false;

    //basic playscreen variables
    private OrthographicCamera gameCam;
    private Viewport gamePort;
    private DirHud dirHud;
    private StrHud strHud;
    private Hud hud;

    //Tiled map variables
    private TmxMapLoader maploader;
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;

    //Box2d variables
    private World world;
    private Box2DDebugRenderer b2dr;
    private B2WorldCreator creator;

    //assets
    private Ball ball;
    private Pin[] pins = new Pin[10];
    private Music music;
    private int score;

    //play variables
    private float dir;
    private float str;
    private Texture turn;
    private Texture winner;


    public PlayScreen(Striker game) {
        this.game = game;

        init();
    }

    private void init() {
        gameCam = new OrthographicCamera();

        //create a FitViewport to maintain virtual aspect ratio despite screen size
        gamePort = new FitViewport(Striker.GAME_WIDTH / Striker.PPM, Striker.GAME_HEIGHT / Striker.PPM, gameCam);

        //create our game HUD for scores/timers/level info
        hud = new Hud(game.getBatch());

        //Load our map and setup our map renderer
        maploader = new TmxMapLoader();
        map = maploader.load("game.tmx");
        music = game.getManager().get("chillsong.ogg", Music.class);
        music.setLooping(true);
        music.setVolume(0.3f);
        music.play();
        renderer = new OrthogonalTiledMapRenderer(map, 1  / Striker.PPM);

        gameCam.position.set(gamePort.getWorldWidth() / 2, gamePort.getWorldHeight() / 2, 0);

        //create our Box2D world, setting no gravity in X, -10 gravity in Y, and allow bodies to sleep
        world = new World(new Vector2(0, 0), true);

        //allows for debug lines of our box2d world.
        //b2dr = new Box2DDebugRenderer();

        creator = new B2WorldCreator(this);

        //create ball in our game world
        ball = new Ball(this);

        //Create pin
        int p = 0;
        int i = 4;
        int stop = 11;
        for(int j = 0; j<4; j++ ) {
            for (int k = i ; k < stop; k += 2) {
                pins[p] = new Pin(this, 16 + (k * Striker.TILE_SIZE), 768 - (int)(1.5 * j * Striker.TILE_SIZE));
                p++;
            }
            System.out.println(j);
            i++;
            stop--;
        }

        turn = new Texture("yourTurn.png");

        world.setContactListener(new WorldContactListener(this));

    }

    @Override
    public void show() {}


    public void handleInput(float dt) {

        if (Gdx.input.justTouched()) {
            if (ball.getCurrentState() == Ball.State.CHARGING) {
                turn = null;
                str = strHud.getStrPointer().getyOffset();
                strHud.dispose();
                ball.getB2Body().applyForce(new Vector2(dir * 5, str * 5), ball.getB2Body().getWorldCenter(), true);
                game.getManager().get("bowl.wav", Sound.class).play();
                ball.setCurrentState(Ball.State.LAUNCHED);
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        int score = 0;
                        for (Pin pin: pins) {
                            if (pin.isPinHit()) {
                                score++;
                            }
                        }
                        System.out.println(score);
                        result(dir, str, score);
                        dispose();
                        game.createWatchingScreen();
                    }
                }, 5);
            }

            if (ball.getCurrentState() == Ball.State.DIRECTING) {
                dir = dirHud.getDirPointer().getxOffset();
                dirHud.dispose();
                strHud = new StrHud(game.getBatch(), this);
                ball.setCurrentState(Ball.State.CHARGING);
            }

            if (ball.getCurrentState() == Ball.State.STOPPED) {
                dirHud = new DirHud(game.getBatch(), this);
                ball.setCurrentState(Ball.State.DIRECTING);
            }
        }

    }

    public void result(float dir, float str, int score){
        String result = score + ":" + Float.toString(dir) + ":" + Float.toString(str);
        game.sendResult(result);

    }

    public void update(float dt){
        //handle user input first
        handleInput(dt);
        world.step(1 / 60f, 6, 2);
        ball.update(dt);
        if (dirHud != null) dirHud.update(dt);
        if (strHud != null) strHud.update(dt);
        for(int i = 0; i < 10; i++){
            pins[i].update(dt);
        }

        if (game.isGameOver()) {
            if (game.getWinner().equals("winner")) {
                winner = new Texture("youWin.png");
                turn.dispose();
                game.getBatch().draw(winner, 0f, 0f, 480f / Striker.PPM, 800f / Striker.PPM);
            } else if (game.getWinner().equals("loser")) {
                winner = new Texture("youLose.png");
                turn.dispose();
                game.getBatch().draw(winner, 0f, 0f, 480f / Striker.PPM, 800f / Striker.PPM);
            } else {
                winner = new Texture("itsaTie.png");
                turn.dispose();
                game.getBatch().draw(winner, 0f, 0f, 480f / Striker.PPM, 800f / Striker.PPM);
            }
        }

        //update our gameCam with correct coordinates after changes
        gameCam.update();
        //tell our renderer to draw only what our camera can see in our game world.
        renderer.setView(gameCam);
    }

    @Override
    public void render(float delta) {
        //separate our update logic from render
        update(delta);


        //Clear the game screen with Black
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //render our game map
        renderer.render();

        //renderer our Box2DDebugLines
        //b2dr.render(world, gameCam.combined);

        //draw everything in the SpriteBatch
        game.getBatch().setProjectionMatrix(gameCam.combined);
        game.getBatch().begin();

        if (dirHud != null && !dirHud.isDisposed()) dirHud.render(delta);
        if (strHud != null && !strHud.isDisposed()) strHud.render(delta);
        ball.draw(game.getBatch());
        for(int i = 0; i < 10; i++){
            pins[i].draw(game.getBatch());
        }
        if (turn != null) game.getBatch().draw(turn, 0f, 0f, 480f / Striker.PPM, 800f / Striker.PPM);
        if (winner != null) game.getBatch().draw(winner, 0f, 0f, 480f / Striker.PPM, 800f / Striker.PPM);

        game.getBatch().end();

        if(gameOver()){
            dispose();
        }
    }

    public void addScore() {
        score++;
        System.out.println(score);
    }

    public boolean gameOver(){

        return false;
    }

    @Override
    public void resize(int width, int height) {
        //updated our game viewport
        gamePort.update(width,height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        //dispose of all our opened resources
        map.dispose();
        renderer.dispose();
        world.dispose();
        //b2dr.dispose();
    }

    public TiledMap getMap() {
        return map;
    }
    public World getWorld() {
        return world;
    }

    public Striker getGame() {
        return game;
    }

    public Hud getHud() {
        return hud;
    }

    public Ball getBall() {
        return ball;
    }

    public Pin[] getPins() {
        return pins;
    }

    public OrthographicCamera getGameCam() {
        return gameCam;
    }

    public OrthogonalTiledMapRenderer getRenderer() {
        return renderer;
    }

    public Texture getTurn() {
        return turn;
    }

    public void setTurn(Texture turn) {
        this.turn = turn;
    }
}
