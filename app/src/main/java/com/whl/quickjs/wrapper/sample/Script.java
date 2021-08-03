package com.whl.quickjs.wrapper.sample;

public class Script {

    public static final String ELEMENT = "function createElement(type, props, ...children) {\n" +
            "    if (children.length === 1) {\n" +
            "        var text = children[0];\n" +
            "        if (typeof text === \"string\") {\n" +
            "            if (props == null) {\n" +
            "                props = {};\n" +
            "            }\n" +
            "            \n" +
            "            props.text = text;\n" +
            "            return {\n" +
            "                type: type,\n" +
            "                props: props\n" +
            "            };\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    return {\n" +
            "        type: type,\n" +
            "        props: props,\n" +
            "        children: children\n" +
            "    };\n" +
            "}\n";

    public static final String RENDER = "function render(data) {\n" +
            "  return createElement(\"column\", {\n" +
            "    style: {\n" +
            "      backgroundColor: \"#121F2C\",\n" +
            "      flex: 1,\n" +
            "      height: \"100%\"\n" +
            "    }\n" +
            "  }, createElement(\"column\", {\n" +
            "    style: {\n" +
            "      flex: 1,\n" +
            "      justifyContent: \"center\",\n" +
            "      alignItems: \"center\"\n" +
            "    }\n" +
            "  }, createElement(\"image\", {\n" +
            "    style: {\n" +
            "      width: 200,\n" +
            "      height: 200\n" +
            "    },\n" +
            "    src: \"https://gw.alicdn.com/tfs/TB1PUJ0xKT2gK0jSZFvXXXnFXXa-200-200.png\"\n" +
            "  }), createElement(\"text\", {\n" +
            "    style: {\n" +
            "      color: \"white\",\n" +
            "      fontSize: 36,\n" +
            "      marginTop: 19\n" +
            "    }\n" +
            "  }, \"陈建国\"), createElement(\"text\", {\n" +
            "    style: {\n" +
            "      color: \"#999999\",\n" +
            "      fontSize: 32,\n" +
            "      marginTop: 19\n" +
            "    }\n" +
            "  }, \"城乡街道心连心便民超市\")), createElement(\"column\", {\n" +
            "    style: {\n" +
            "      flex: 1,\n" +
            "      justifyContent: \"space-between\",\n" +
            "      alignItems: \"center\"\n" +
            "    }\n" +
            "  }, createElement(\"column\", {\n" +
            "    style: {\n" +
            "      justifyContent: \"center\",\n" +
            "      alignItems: \"center\"\n" +
            "    }\n" +
            "  }, createElement(\"text\", {\n" +
            "    style: {\n" +
            "      color: \"white\",\n" +
            "      fontSize: 36,\n" +
            "      marginTop: 19\n" +
            "    }\n" +
            "  }, \"请先接听来电\"), createElement(\"text\", {\n" +
            "    style: {\n" +
            "      color: \"#D8D8D8\",\n" +
            "      fontSize: 32,\n" +
            "      marginTop: 19\n" +
            "    }\n" +
            "  }, \"随后将自动拨打对方\")), createElement(\"image\", {\n" +
            "    onClick: \"close\",\n" +
            "    style: {\n" +
            "      width: 140,\n" +
            "      height: 140,\n" +
            "      marginBottom: 100\n" +
            "    },\n" +
            "    src: \"https://gw.alicdn.com/tfs/TB1WrBaxuT2gK0jSZFvXXXnFXXa-142-142.png\"\n" +
            "  })));\n" +
            "}\n";

}
