 

import org.lwjgl.sdl.*;
import static org.lwjgl.sdl.SDLSurface.*;
import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLIOStream.*;
import static org.lwjgl.sdl.SDLPixels.*;
import static org.lwjgl.sdl.SDLRender.*;



class FontRender {
    SDL_Texture texture;
    long ren;
    String[] format = {"ABCDEFGHIJKLM", "NOPQRSTUVWXYZ", "abcdefghijklm", "nopqrstuvwxyz", "1234567890"};
    SDL_Rect charSize;
    int xPad = 0, yPad = 0;
    int scale = 1;

    public FontRender(String image, SDL_Rect size, long renderer){
        ren = renderer;
        charSize = size;
        loadTexture(image);
    }

    private void loadTexture(String location){
            long io = SDL_IOFromFile(location, "rb");

            if (io == 0L) {
                System.out.println("Could not open file: " + location);
                System.out.println(SDL_GetError());
                return;
            }

            SDL_Surface img = SDL_LoadPNG_IO(io, true);

        if (img == null) {
            System.out.println("Failed to load image: " + SDL_GetError());
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

        for(int i = 0; i < format.length; i++){
            String f = format[i];
            if(f.indexOf(c) >= 0){
                cut.set(f.indexOf(c)*(charSize.w()+xPad)+charSize.x(), (charSize.h()+yPad)*i+charSize.y(), charSize.w(), charSize.h());
                break;
            }else if(c == ' '){
                return true;
            }
        }

        SDL_RenderTexture(ren, texture, cut, pos);
        return false;
    }
}
