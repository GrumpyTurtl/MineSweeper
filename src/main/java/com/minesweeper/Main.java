package com.minesweeper;

import org.lwjgl.*;
import org.lwjgl.sdl.*;


import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLRender.*;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLMouse.*;
import static org.lwjgl.sdl.SDLTimer.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;


public class Main {
    public static void main(String[] args) {
        init();
    }


    static enum type{
        EMPTY,
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        BLANK,
        MINE,
        FLAG
    }

    final static int GRID_SIZE = 16, TILE_SIZE = 32;
    final static int WIDTH = TILE_SIZE*GRID_SIZE, HEIGHT = TILE_SIZE*GRID_SIZE+32;
    static int MINECOUNT = 20;
    
    static PointerBuffer window, renderer;
    static long ren, win;

    static String[] imgs = {"Empty", "1", "2", "3", "4", "5", "6", "7", "8", "Blank", "Mine", "Flag"};


    static int[][] viewGrid = new int[GRID_SIZE][GRID_SIZE];
    static int[][] hiddenGrid = new int[GRID_SIZE][GRID_SIZE];

    static int safeTiles = 0;
    static int flaggedMines = 0;

    static ArrayList<Integer> cascadeQueueX = new ArrayList<>();
    static ArrayList<Integer> cascadeQueueY = new ArrayList<>();
    
    static float mouseX, mouseY;
    static int timer = 0;
    static SDL_TimerCallbackI callback;
  

    public static void init(){
        SDL_Init(SDL_INIT_VIDEO);
        window = PointerBuffer.allocateDirect(16);
        renderer = PointerBuffer.allocateDirect(16);


        if(!SDL_CreateWindowAndRenderer("Minesweeper", WIDTH, HEIGHT, NULL, window, renderer)){
            System.out.println("Couldnt create window");
        }
        ren = renderer.get();
        win = window.get();

        SDL_Rect charSize = SDL_Rect.create().set(4,2,12,17);
        FontRender font = new FontRender("font", charSize, ren);
        font.yPad = 3;
        font.xPad = 0;

        viewGrid = fill(0, GRID_SIZE);
        
        SDL_Texture[] textures = makeTextures();
        SDL_FRect size = SDL_FRect.create();


        SDL_Event event = SDL_Event.calloc();
        boolean quit = false;
        boolean start = false;

        
        callback = (userdata, timerID, interval) -> {
            timer += 1;
            SDL_AddTimer(1000, callback, 0);
            return 0; 
        };

        SDL_AddTimer(1000, callback, 0);
        
        //double deltaTime = 0;
  
        while(!quit){
            SDL_UpdateWindowSurface(win);
            
            //deltaTime = (double)((now-last)*1000 / (double)SDL_GetPerformanceFrequency());

            while(SDL_PollEvent(event)){
                switch (event.type()) {
                    case SDL_EVENT_QUIT -> quit = true;
                    case SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                        if(event.button().button() == SDL_BUTTON_LEFT){
                            mouseX = event.motion().x();
                            mouseY = event.motion().y();
                            int gridX = (int)Math.floor(mouseY/TILE_SIZE)-1;
                            int gridY = (int)Math.floor(mouseX/TILE_SIZE);
                            if(gridX >= 0 && gridX < GRID_SIZE && gridY >= 0 && gridY < GRID_SIZE){
                                if(!start) {
                                    start = true;         
                                    fillMines(MINECOUNT, gridX, gridY);
                                    populateGrid();
                                }

                                if(viewGrid[gridX][gridY] == 0){
                                    cascadeQueueX.add(gridX);
                                    cascadeQueueY.add(gridY);
                                }else if(viewGrid[gridX][gridY] == type.FLAG.ordinal()){
                                    viewGrid[gridX][gridY] = 0;
                                }
                            }
                            //System.out.printf("left click x: %f, y: %f GridX: %d, GridY: %d\n", mouseX, mouseY, gridX, gridY);
                        }else if(event.button().button() == SDL_BUTTON_RIGHT){
                            mouseX = event.motion().x();
                            mouseY = event.motion().y();
                            int gridX = (int)Math.floor(mouseY/TILE_SIZE);
                            int gridY = (int)Math.floor(mouseX/TILE_SIZE);
                            if(gridX >= 0 && gridX < GRID_SIZE && gridY >= 0 && gridY < GRID_SIZE){

                                if(viewGrid[gridX][gridY] == 0){
                                    viewGrid[gridX][gridY] = type.FLAG.ordinal(); 
                                }else if(viewGrid[gridX][gridY] == type.FLAG.ordinal()){
                                    viewGrid[gridX][gridY] = 0;
                                }
                            }
                            //System.out.printf("right click x: %f, y: %f \n", mouseX, mouseY);
                        }
                    }
                }
            }   
            if(SDL_GetPerformanceCounter() % 500 <= 10){
                int initialLength = cascadeQueueX.size();
                for(int i = 0; i < initialLength; i++){
                    cascadeTiles(cascadeQueueX.get(i), cascadeQueueY.get(i));
                }
                cascadeQueueX = removeRange(cascadeQueueX, 0, initialLength);
                cascadeQueueY = removeRange(cascadeQueueY, 0, initialLength);
            }

            SDL_SetRenderDrawColor( ren, (byte)192, (byte)192, (byte)192, (byte)255);
            SDL_RenderClear(ren);
            size.set(0,32,TILE_SIZE,TILE_SIZE);
            safeTiles = 0;
            flaggedMines = 0;
            for(int x = 0; x < GRID_SIZE; x++){
                for(int y = 0; y < GRID_SIZE; y++){
                    int texValue = (viewGrid[x][y] == 1) ? hiddenGrid[x][y] : type.BLANK.ordinal();
                    texValue = (viewGrid[x][y] == type.FLAG.ordinal()) ? viewGrid[x][y] : texValue;
                    SDL_RenderTexture(ren, textures[texValue], null, size);

                    if(texValue == type.MINE.ordinal()) quit = true;
                    if(hiddenGrid[x][y] != type.MINE.ordinal() && viewGrid[x][y] == 0) safeTiles++;
                    if(hiddenGrid[x][y] == type.MINE.ordinal() && viewGrid[x][y] == type.FLAG.ordinal()) flaggedMines++;

                    String str = "Timer " + timer;
                    font.RenderString(16,8,str);

                    SDL_FRect pos = SDL_FRect.create();
                    pos.set(WIDTH-TILE_SIZE*2, 0, TILE_SIZE, TILE_SIZE);
                    SDL_RenderTexture(ren, textures[type.FLAG.ordinal()], null, pos);
                    font.RenderString(WIDTH-TILE_SIZE+4, 8, "" + (15-flaggedMines));

                    size.x(size.x() + TILE_SIZE);
                    if(size.x() >= GRID_SIZE*TILE_SIZE){
                        size.x(0);
                        size.y(size.y() + TILE_SIZE);
                    }
                }
            }

            if(flaggedMines == MINECOUNT || safeTiles == 0){
                System.out.println("you win!");
            }
            SDL_RenderPresent(ren);
        }

        for(SDL_Texture tex: textures){
            SDL_DestroyTexture(tex);
        }
        SDL_DestroyRenderer(ren);
        SDL_DestroyWindow(win);
        SDL_Quit();
    }

    private static SDL_Texture[] makeTextures(){
        SDL_Texture[] out = new SDL_Texture[imgs.length];
        for(int i = 0; i < imgs.length; i++){
            String name = imgs[i];
            SDL_Texture texture;
            SDL_Surface bmp;
            try{
                URI url = Main.class.getResource("/" + name + ".bmp").toURI();
                bmp = SDL_LoadBMP(new File(url).getAbsolutePath());

                if (bmp == null) {
                    System.out.println("Failed to load BMP: " + SDL_GetError());
                    continue;
                }             

                texture = SDL_CreateTextureFromSurface(ren, bmp);
                SDL_DestroySurface(bmp);

                if (texture == null) {
                    System.out.println("Failed to Load Texture: " + SDL_GetError());
                }else{
                    out[i] = texture;
                }       

            }catch(URISyntaxException e){
                System.out.println("error loading Texture: " + e);
            }
        }

        return out;
    }


    static int[][] fill(int value, int size){
        int[][] out = new int[size][size];
        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                out[x][y] = value;
            }
        }
        return out;
    }

    static void fillMines(int mineCount, int safeX, int safeY){
        for(int i = 0; i < mineCount; i++){
            int x = randomInt(0, GRID_SIZE-1);
            int y = randomInt(0, GRID_SIZE-1);
            if(hiddenGrid[x][y] == type.MINE.ordinal() || (x >= safeX-1 && x <= safeX+1 && y >= safeY-1 && y <= safeY+1)){
                i--;
            }else{
                hiddenGrid[x][y] = type.MINE.ordinal();
            }
        }
    }

    static int randomInt(int min, int max){
        return (int)Math.floor(Math.random() * (max-min +1)) + min;
    }

    static void populateGrid(){
        for(int y = 0; y < GRID_SIZE; y++){
            for(int x = 0; x < GRID_SIZE; x++){
                if(hiddenGrid[x][y] != type.MINE.ordinal()){
                    hiddenGrid[x][y] = checkSurrounding(x,y);
                }
            }
        }
    }

    static int checkSurrounding(int x, int y){ 
        int count = 0;
        for(int y2 = y-1; y2 <= (y+1); y2++){
            for(int x2 = x-1; x2 <= (x+1); x2++){
                if(x2 >= 0 && x2 < GRID_SIZE && y2 >= 0 && y2 < GRID_SIZE ){
                    if(x2 == x && y2 == y) continue;

                    if(hiddenGrid[x2][y2] == type.MINE.ordinal()){
                        count++;
                    }
                }
            }
        }
        return count;
    }

    static ArrayList<Integer> removeRange(ArrayList<Integer> a, int s, int e){
        ArrayList<Integer> out = new ArrayList<>();
        if(s > 0){
            for(int i = 0; i < s; i++){
                out.add(a.get(i));
            }
        }

        for(int i = e; i < a.size(); i++){
                out.add(a.get(i));
        }

        return out;
    }

    /*  static void printIntArray(int[][] a){
        for(int[] b: a){
            for(int n: b){
                System.out.printf("%d ", n);
            }
            System.out.printf("\n");
        }
        System.out.printf("\n \n");
    }*/

    static void cascadeTiles(int x, int y){
        viewGrid[x][y] = 1;
        /*int mineCount = 0;
        for(int y2 = y-1; y2 <= (y+1); y2++){
            for(int x2 = x-1; x2 <= (x+1); x2++){
                if(x2 >= 0 && x2 < GRID_SIZE && y2 >= 0 && y2 < GRID_SIZE ){
                    if(x2 == x && y2 == y) continue;

                    if(hiddenGrid[x2][y2] == type.MINE.ordinal()){
                        mineCount++;
                    }
                    
                }
            }
        }*/
       int mineCount = checkSurrounding(x,y);
        if(mineCount == 0){
            int[] xTiles = {x-1, x, x+1, x-1, x+1, x-1, x, x+1};
            int[] yTiles = {y-1, y-1, y-1, y, y, y+1, y+1, y+1};
            for(int i = 0; i < xTiles.length; i++){
                if(xTiles[i] >= 0 && xTiles[i] < GRID_SIZE && yTiles[i] >= 0 && yTiles[i] < GRID_SIZE){
                    if(viewGrid[xTiles[i]][yTiles[i]] == 1) continue;   
                    cascadeQueueX.add(xTiles[i]);
                    cascadeQueueY.add(yTiles[i]);
                }
            }
        }
    }
}