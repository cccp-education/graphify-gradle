package graphify.schema

enum class NodeType(val wire: String) {
    PROJECT("project"),
    DIRECTORY("directory"),
    FILE("file"),
    SECTION("section")
}
