package com.minesweeper;

import org.lwjgl.*;
import org.lwjgl.sdl.*;
import static org.lwjgl.sdl.SDLError.*;


import static org.lwjgl.sdl.SDLRender.*;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLVideo.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.File;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.management.openmbean.OpenDataException;


public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World!");
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
    final static int WIDTH = TILE_SIZE*GRID_SIZE, HEIGHT = TILE_SIZE*GRID_SIZE;
    
    static PointerBuffer window, renderer;
    static long ren, win;

    static String[] imgs = {"Empty", "1", "2", "3", "4", "5", "6", "7", "8", "Blank", "Mine", "Flag"};


    static int[][] viewGrid = new int[GRID_SIZE][GRID_SIZE];
    static int[][] hiddenGrid = new int[GRID_SIZE][GRID_SIZE];

    public static void init(){
        SDL_Init(SDL_INIT_VIDEO);
        window = PointerBuffer.allocateDirect(16);
        renderer = PointerBuffer.allocateDirect(16);

        if(!SDL_CreateWindowAndRenderer("Minesweeper", WIDTH, HEIGHT, NULL, window, renderer)){
            System.out.println("Couldnt create window");
        }
        ren = renderer.get();
        win = window.get();

        viewGrid = fill(1, GRID_SIZE);
        fillMines(5);
        populateGrid();

        
        SDL_Texture[] textures = makeTextures();
        SDL_FRect size = SDL_FRect.create();

        SDL_Event event = SDL_Event.calloc();
        boolean quit = false;


        printIntArray(viewGrid);
        printIntArray(hiddenGrid);
  
        while(!quit){
            SDL_UpdateWindowSurface(win);
            while(SDL_PollEvent(event)){
                switch (event.type()) {
                    case SDL_EVENT_QUIT:
                        quit = true;
                        break;
                }
            }   

            SDL_RenderClear(ren);
            size.set(0,0,TILE_SIZE,TILE_SIZE);
            for(int x = 0; x < GRID_SIZE; x++){
                for(int y = 0; y < GRID_SIZE; y++){
                    int texValue = (viewGrid[x][y] == 1) ? hiddenGrid[x][y] : type.BLANK.ordinal();
                    SDL_RenderTexture(ren, textures[texValue], null, size);

                    size.x(size.x() + TILE_SIZE);
                    if(size.x() >= GRID_SIZE*TILE_SIZE){
                        size.x(0);
                        size.y(size.y() + TILE_SIZE);
                    }
                }
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
                e.printStackTrace();
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

    static void fillMines(int mineCount){
        for(int i = 0; i < mineCount; i++){
            int x = randomInt(0, GRID_SIZE-1);
            int y = randomInt(0, GRID_SIZE-1);
            if(hiddenGrid[x][y] == type.MINE.ordinal()){
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

    static void printIntArray(int[][] a){
        for(int[] b: a){
            for(int n: b){
                System.out.printf("%d ", n);
            }
            System.out.printf("\n");
        }
        System.out.printf("\n \n");
    }
}