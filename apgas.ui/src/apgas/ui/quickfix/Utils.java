package apgas.ui.quickfix;

public class Utils {
  public static boolean isAPGAS(String name) {
    switch (name) {
    // Constructs
    case "finish":
      break;
    case "async":
      break;
    case "asyncAt":
      break;
    case "uncountedAsyncAt":
      break;
    case "at":
      break;
    case "here":
      break;
    case "place":
      break;
    case "places":
      break;
    // APGAS Classes
    case "Configuration":
      break;
    case "Constructs":
      break;
    case "DeadPlaceException":
      break;
    case "GlobalRuntime":
      break;
    case "MultipleException":
      break;
    case "Place":
      break;
    default:
      return false;
    }
    return true;
  }
}
