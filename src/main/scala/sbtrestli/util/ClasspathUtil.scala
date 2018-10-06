package sbtrestli.util

object ClasspathUtil {

  /**
   * inspired by play.api.util.Threads method with same name.
   * added finally to be exception safe
   *
   * @param classloader
   * @param res
   * @tparam T
   * @return
   */
  def withContextClassLoader[T](classloader: ClassLoader)(res: => T): T = {
    val thread = Thread.currentThread
    val oldLoader = thread.getContextClassLoader
    thread.setContextClassLoader(classloader)
    try {
      res
    } finally {
      thread.setContextClassLoader(oldLoader)
    }
  }

  /**
   * Create a ClassLoader from a collection of classpath URLs.
   *
   * @param classpath
   * @param parentClassLoader
   * @return
   */
  def classLoaderFromClasspath(classpath: Seq[String], parentClassLoader: ClassLoader = this.getClass.getClassLoader): ClassLoader = {
    val classUrls = classpath.map(path => new java.io.File(path).toURI.toURL).toArray
    new java.net.URLClassLoader(classUrls, parentClassLoader)
  }
}
