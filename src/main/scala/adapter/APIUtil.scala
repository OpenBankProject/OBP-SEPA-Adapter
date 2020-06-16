package adapter

object APIUtil {
  def gitCommit: String = {
    try {
      val properties = new java.util.Properties()
      properties.load(getClass.getClassLoader.getResourceAsStream("git.properties"))
      properties.getProperty("git.commit.id", "")
    } catch {
      case e: Throwable => ""
    }
  }
}
