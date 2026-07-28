package com.minesweeper;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.lwjgl.sdl.*;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLPixels.*;
import static org.lwjgl.sdl.SDLRender.*;
//import static org.lwjgl.system.MemoryUtil.NULL;


class FontRender {
    SDL_Texture texture;
    long ren;
    String[] format = {"ABCDEFGHIJKLM", "NPOQRSTUVWXYZ", "abcdefghijklm", "nopqrstuvwxyz", "1234567890"};
    SDL_Rect charSize;
    int xPad = 0, yPad = 0;
    int scale = 1;

    public FontRender(String image, SDL_Rect size, long renderer){
        ren = renderer;
        charSize = size;
        loadTexture(image);
    }

    private void loadTexture(String location){
        SDL_Surface img;
        try{
            URI url = Main.class.getResource("/" + location + ".png").toURI();
            img = SDL_LoadPNG(new File(url).getAbsolutePath());
            
            if (img == null) {
                    System.out.println("Failed to load BMP: " + SDL_GetError());
                    return;
                }             

            SDL_PixelFormatDetails details = SDL_GetPixelFormatDetails(img.format());
            int key = SDL_MapRGB(details, null, (byte)255, (byte)255, (byte)255);
            SDL_SetSurfaceColorKey(img, true, key);
            texture = SDL_CreateTextureFromSurface(ren, img);
            SDL_DestroySurface(img);

            if (texture == null) {
                System.out.println("Failed to Load Texture: " + SDL_GetError());
            }

        }catch(URISyntaxException e){
            
        }
    }

    public void RenderString(int x, int y, String s){
        int penX = x;
        int penY = y;
        for(int i = 0; i < s.length(); i++){
            if(i+1 < s.length() && s.substring(i, i+1).equals("\n")){
                penX = x;
                penY += charSize.h()*scale;
            }else{
                if(RenderChar(penX, penY, s.charAt(i))){
                    penX += charSize.w()*scale;
                }else{
                    penX += charSize.w()*scale;
                }
            }
        }
    }

    public boolean RenderChar(int x, int y, char c){
        SDL_FRect pos = SDL_FRect.create();
        SDL_FRect cut = SDL_FRect.create();
        pos.set(x, y, charSize.w()*scale, charSize.h()*scale);

        /*if(format[0].indexOf(c) >= 0){
            cut.set(format[0].indexOf(c)*(charSize.w()+xPad), yPad, charSize.w(), charSize.h());
        }else if (format[1].indexOf(c) >= 0){
            cut.set(format[1].indexOf(c)*(charSize.w()+xPad), charSize.h()+yPad, charSize.w(), charSize.h());

        }else if (format[2].indexOf(c) >= 0){
            cut.set(format[2].indexOf(c)*(charSize.w()+xPad), (charSize.h()+yPad)*2, charSize.w(), charSize.h());
        }else if(c == ' '){
            return;
        }*/

        for(int i = 0; i < format.length; i++){
            String f = format[i];
            if(f.indexOf(c) >= 0){
                cut.set(f.indexOf(c)*(charSize.w()+xPad)+charSize.x(), (charSize.h()+yPad)*i+charSize.y(), charSize.w(), charSize.h());
                break;
            }else if(c == ' '){
                return true;
            }
        }

        //System.out.println("x: " + cut.x() + " y: " + cut.y());
        
        SDL_RenderTexture(ren, texture, cut, pos);
        return false;
    }
}