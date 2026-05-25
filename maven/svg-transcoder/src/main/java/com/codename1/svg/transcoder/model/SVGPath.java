package com.codename1.svg.transcoder.model;

import com.codename1.svg.transcoder.parser.PathCommand;

import java.util.List;

public final class SVGPath extends SVGShape {
    private List<PathCommand> commands;

    public List<PathCommand> getCommands() { return commands; }
    public void setCommands(List<PathCommand> commands) { this.commands = commands; }
}
