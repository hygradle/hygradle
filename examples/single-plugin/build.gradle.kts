import dev.hygradle.internal.HytalePatchline

plugins { id("dev.hygradle") }

hygradle.hytale {
  version = "2026.01.28-87d03be09"
  patchline = HytalePatchline.RELEASE

  plugin("examplePlugin") { manifest {} }
  run("examplePlugin") {}
}
