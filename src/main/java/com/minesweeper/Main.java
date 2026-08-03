package com.minesweeper;

import org.lwjgl.*;
import org.lwjgl.sdl.*;
import java.util.ArrayList;

import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLRender.*;
import static org.lwjgl.sdl.SDLBlendMode.*;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLMouse.*;
import static org.lwjgl.sdl.SDLTimer.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.sdl.SDLIOStream.*;

class Button{
    SDL_FRect rect;
    String buttonText;
    FontRender font;
    int offsetX, offsetY;

    Button(int _x, int _y, int _w, int _h, FontRender _font,  String str){
        buttonText = str;

        rect = SDL_FRect.create();
        rect.set(_x,_y,_w,_h);

        font = _font;
        offsetX = (_w/2)-(font.charSize.w()*(str.length()/2));
        offsetY = (_h/2) - (font.charSize.h()/2);
    }

    void RenderButton(long ren){
        //replace with dynamic image based button?
        SDL_RenderFillRect(ren, rect);
        font.RenderString((int)(rect.x()+offsetX), (int)(rect.y()+offsetY), buttonText);
    }
}


public class Main {
    @SuppressWarnings("UnnecessaryReturnStatement")
    public static void main(String[] args) {
        SDL_Init(SDL_INIT_VIDEO);
        window = PointerBuffer.allocateDirect(16);
        renderer = PointerBuffer.allocateDirect(16);

        if(!SDL_CreateWindowAndRenderer("Minesweeper", WIDTH, HEIGHT, NULL, window, renderer)){
            System.out.println("Couldnt create window");
        }
        ren = renderer.get();
        win = window.get();

        SDL_Rect charSize = SDL_Rect.create().set(4,2,12,17);
        font = new FontRender("assets/font.png", charSize, ren);
        font.yPad = 3;
        font.xPad = 0;

        textures = makeTextures();

        game();
        return;
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
    final static int CASCADING_TILE_SPEED = 20;
    final static int MINECOUNT = 1;
    
    static PointerBuffer window, renderer;
    static long ren, win;

    static String[] imgs = {"Empty", "1", "2", "3", "4", "5", "6", "7", "8", "Blank", "Mine", "Flag"};


    static int[][] viewGrid = new int[GRID_SIZE][GRID_SIZE];
    static int[][] hiddenGrid = new int[GRID_SIZE][GRID_SIZE];

    static int safeTiles = 0;
    static int flaggedMines = 0;
    static int flagsLeft = MINECOUNT;

    static ArrayList<Integer> cascadeQueueX = new ArrayList<>();
    static ArrayList<Integer> cascadeQueueY = new ArrayList<>();
    
    static float mouseX, mouseY;
    static int timer = 0;
    static boolean timerStop = false;
    static SDL_TimerCallbackI callback;
    static boolean gameOver = false;
    static byte difficulty = 1;


    static FontRender font;
    static SDL_Texture[] textures;

    public static void game(){

        viewGrid = new int[GRID_SIZE][GRID_SIZE];
        hiddenGrid = new int[GRID_SIZE][GRID_SIZE];

        viewGrid = fill(0, GRID_SIZE);

        cascadeQueueY = new ArrayList<>();
        cascadeQueueX = new ArrayList<>();
        
        boolean quit = false;
        boolean startTile = false;
        boolean showMenu = true;

        timer = 0;
        timerStop = false;
        gameOver = false;

        safeTiles = 0;
        flaggedMines = 0;
        flagsLeft = MINECOUNT;

        
        Button retryButton = new Button(WIDTH/2-64, HEIGHT/2-16, 128, 32, font, "Retry");
        Button quitButton = new Button(WIDTH/2-64, HEIGHT/2+32, 128, 32, font, "Quit");
        Button playButton = new Button(WIDTH/2-64, HEIGHT/2-16, 128, 32, font, "Play");
        Button difficultyButton = new Button(WIDTH/2-64, HEIGHT/2+32, 128, 32, font, "Medium");
        SDL_FRect size = SDL_FRect.create();
        SDL_Event event = SDL_Event.calloc();

        String[] difficulties = {"Easy", "Medium", "Hard"};


        //time
        long now = SDL_GetTicks();
        long last;
        long elapsedTime;
        int timeBuffer = 0;

        callback = (userdata, timerID, interval) -> {
            if(!timerStop){
                timer += 1;
                SDL_AddTimer(1000, callback, 0);
            }
            return 0; 
        };

        
        
        //game loop
        while(!quit){
            SDL_UpdateWindowSurface(win);
            last = now;
            now = SDL_GetTicks();
            elapsedTime = now - last;

            while(SDL_PollEvent(event)){
                switch (event.type()) {
                    case SDL_EVENT_QUIT -> quit = true;
                    case SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                        if(event.button().button() == SDL_BUTTON_LEFT && !gameOver  && !showMenu){
                            mouseX = event.motion().x();
                            mouseY = event.motion().y();
                            int gridX = (int)Math.floor(mouseY/TILE_SIZE)-1;
                            int gridY = (int)Math.floor(mouseX/TILE_SIZE);
                            if(gridX >= 0 && gridX < GRID_SIZE && gridY >= 0 && gridY < GRID_SIZE){
                                if(!startTile) {
                                    startTile = true;         
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
                            
                        }else if(event.button().button() == SDL_BUTTON_RIGHT && !gameOver && !showMenu){
                            mouseX = event.motion().x();
                            mouseY = event.motion().y();
                            int gridX = (int)Math.floor(mouseY/TILE_SIZE)-1;
                            int gridY = (int)Math.floor(mouseX/TILE_SIZE);
                            if(gridX >= 0 && gridX < GRID_SIZE && gridY >= 0 && gridY < GRID_SIZE){
                                if(viewGrid[gridX][gridY] == 0 && flagsLeft > 0){
                                    viewGrid[gridX][gridY] = type.FLAG.ordinal(); 
                                    flagsLeft--;
                                }else if(viewGrid[gridX][gridY] == type.FLAG.ordinal()){
                                    viewGrid[gridX][gridY] = 0;
                                    flagsLeft++;
                                }
                            }
                            
                        }else if (event.button().button() == SDL_BUTTON_LEFT && (gameOver || showMenu)) {
                            mouseX = event.motion().x();
                            mouseY = event.motion().y();
                            Button[] buttonList = {quitButton, retryButton, playButton, difficultyButton};
                            for(int i = 0; i < buttonList.length; i++){
                                Button b = buttonList[i];
                                if (mouseX > b.rect.x() && mouseX < b.rect.x()+b.rect.w()){
                                    if (mouseY > b.rect.y() && mouseY < b.rect.y()+b.rect.h()){
                                        switch (i) {
                                            case 0:
                                                quit = true;
                                                System.out.println("quit");
                                                break;
                                            case 1:
                                                game();
                                                System.out.println("rety");
                                                break;
                                            case 2:
                                                showMenu = false;
                                                SDL_AddTimer(1000, callback, 0);
                                                gameOver = false;
                                                System.out.println("play");
                                                break;
                                            case 3:
                                                difficulty++;
                                                if(difficulty > 2) difficulty = 0;
                                                System.out.println("difficulty");
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }  



            timeBuffer += elapsedTime;
            if(timeBuffer > CASCADING_TILE_SPEED){
                timeBuffer = 0;
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
                    //draw textures
                    int texValue = (viewGrid[x][y] == 1) ? hiddenGrid[x][y] : type.BLANK.ordinal();
                    texValue = (viewGrid[x][y] == type.FLAG.ordinal()) ? viewGrid[x][y] : texValue;
                    SDL_RenderTexture(ren, textures[texValue], null, size);

                    // win/lose check
                    if(hiddenGrid[x][y] != type.MINE.ordinal() && viewGrid[x][y] == 0) safeTiles++;
                    if(hiddenGrid[x][y] == type.MINE.ordinal() && viewGrid[x][y] == type.FLAG.ordinal()) flaggedMines++;

                    if (texValue == type.MINE.ordinal()) {
                        timerStop = true;
                        gameOver = true;
                    }
                    //timer
                    String str = "Timer " + timer;
                    font.RenderString(16,8,str);
                    
                    //flag Count
                    SDL_FRect pos = SDL_FRect.create();
                    pos.set(WIDTH-TILE_SIZE*2, 0, TILE_SIZE, TILE_SIZE);
                    SDL_RenderTexture(ren, textures[type.FLAG.ordinal()], null, pos);
                    font.RenderString(WIDTH-TILE_SIZE+4, 8, "" + (flagsLeft));

                    size.x(size.x() + TILE_SIZE);
                    if(size.x() >= GRID_SIZE*TILE_SIZE){
                        size.x(0);
                        size.y(size.y() + TILE_SIZE);
                    }
                }
            }


            if(showMenu){
                SDL_SetRenderDrawBlendMode(ren, SDL_BLENDMODE_BLEND);
                    SDL_FRect tmp = SDL_FRect.create();
                    tmp.set(0,0,WIDTH,HEIGHT);
                    SDL_SetRenderDrawColor(ren, (byte)255, (byte)255, (byte)255, (byte)100);
                    SDL_RenderFillRect(ren, tmp);
                SDL_SetRenderDrawBlendMode(ren, SDL_BLENDMODE_NONE);

                font.scale = 3;
                font.RenderString(WIDTH/2-(int)(font.charSize.w()*font.scale*5.5), HEIGHT/2-(font.charSize.h()*font.scale/2)-font.charSize.h()*font.scale, "Minesweeper");
                font.scale = 1;


                
                difficultyButton.buttonText = difficulties[difficulty];
                

                SDL_SetRenderDrawColor(ren, (byte)148, (byte)148, (byte)148, (byte)255);
                playButton.RenderButton(ren);
                difficultyButton.RenderButton(ren);
            }



            if(flaggedMines == MINECOUNT || safeTiles == 0){
                timerStop = true;
                gameOver = true;

                SDL_SetRenderDrawBlendMode(ren, SDL_BLENDMODE_BLEND);
                    SDL_FRect tmp = SDL_FRect.create();
                    tmp.set(0,0,WIDTH,HEIGHT);
                    SDL_SetRenderDrawColor(ren, (byte)255, (byte)255, (byte)255, (byte)100);
                    SDL_RenderFillRect(ren, tmp);
                SDL_SetRenderDrawBlendMode(ren, SDL_BLENDMODE_NONE);

                font.scale = 3;
                font.RenderString(WIDTH/2-(int)(font.charSize.w()*font.scale*3.5), HEIGHT/2-(font.charSize.h()*font.scale/2)-font.charSize.h()*font.scale, "You Win");
                font.scale = 1;

                SDL_SetRenderDrawColor(ren, (byte)148, (byte)148, (byte)148, (byte)255);
                retryButton.RenderButton(ren);
                quitButton.RenderButton(ren);
            }

            if(gameOver && flaggedMines != MINECOUNT && safeTiles != 0){
                SDL_SetRenderDrawBlendMode(ren, SDL_BLENDMODE_BLEND);
                    SDL_FRect tmp = SDL_FRect.create();
                    tmp.set(0,0,WIDTH,HEIGHT);
                    SDL_SetRenderDrawColor(ren, (byte)255, (byte)255, (byte)255, (byte)100);
                    SDL_RenderFillRect(ren, tmp);
                SDL_SetRenderDrawBlendMode(ren, SDL_BLENDMODE_NONE);

                font.scale = 3;
                font.RenderString(WIDTH/2-(int)(font.charSize.w()*font.scale*3.5), HEIGHT/2-(font.charSize.h()*font.scale/2)-font.charSize.h()*font.scale, "You Lose");
                font.scale = 1;

                SDL_SetRenderDrawColor(ren, (byte)148, (byte)148, (byte)148, (byte)255);
                retryButton.RenderButton(ren);
                quitButton.RenderButton(ren);
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

            long io = SDL_IOFromFile("assets/" + name + ".bmp", "rb");

            if (io == 0L) {
                System.out.println("Could not open file: " + "assets/" + name + ".bmp");
                System.out.println(SDL_GetError());
                continue;
            }

            bmp = SDL_LoadBMP_IO(io, true);

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

    static void cascadeTiles(int x, int y){
        viewGrid[x][y] = 1;
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