import ell.Primitives;
import static ell.Primitives.*;

public class Main {
    
    public static void main(String [] args) {
        if (args.length > 0) {
            runModule(args[0], Primitives.class);
        } else {
            println("REPL NYI. Provide a filename");
        }
    }
}
