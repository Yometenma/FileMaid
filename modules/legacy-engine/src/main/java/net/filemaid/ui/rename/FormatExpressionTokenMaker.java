package net.filemaid.ui.rename;

import javax.swing.text.Segment;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenMakerBase;
import org.fife.ui.rsyntaxtextarea.modes.GroovyTokenMaker;

public class FormatExpressionTokenMaker
extends TokenMakerBase {
    public static final int LANGUAGE_LITERAL = 0;
    public static final int LANGUAGE_GROOVY = 1;
    private final GroovyExpressionTokenMaker groovyTokenMaker = new GroovyExpressionTokenMaker();

    public Token getTokenList(Segment segment, int n, int n2) {
        this.resetTokenList();
        this.groovyTokenMaker.reset();
        int n3 = this.getInitialLevel(segment, n, n2);
        this.setLanguageIndex(n3 > 0 ? 1 : 0);
        if (n3 == 0 && segment.length() >= 8 && '@' == segment.charAt(0) && segment.toString().endsWith(".groovy")) {
            this.addToken(segment, segment.getBeginIndex(), segment.getBeginIndex() + segment.length() - 1, 34, n2);
            this.addNullToken();
            return this.firstToken;
        }
        int n4 = 0;
        int n5 = 0;
        block7: for (int i = 0; i < segment.length(); ++i) {
            switch (segment.charAt(i)) {
                case '{': {
                    if (n3 <= 0) {
                        if (n4 != n5) {
                            this.addToken(segment, segment.getBeginIndex() + n4, segment.getBeginIndex() + n5 - 1, 33, n2 + n4);
                        }
                        this.setLanguageIndex(1);
                        this.addToken(segment, segment.getBeginIndex() + i, segment.getBeginIndex() + i, 32, n2 + i);
                        n4 = n5 = i + 1;
                        n3 = 0;
                    } else {
                        ++n5;
                    }
                    ++n3;
                    continue block7;
                }
                case '}': {
                    if (n3 == 1) {
                        if (n4 != n5) {
                            Segment segment2 = new Segment(segment.array, segment.getBeginIndex() + n4, n5 - n4);
                            this.addToken(this.groovyTokenMaker.getTokenList(segment2, n, n2 + n4));
                        }
                        this.addToken(segment, segment.getBeginIndex() + i, segment.getBeginIndex() + i, 32, n2 + i);
                        this.setLanguageIndex(0);
                        n4 = n5 = i + 1;
                    } else {
                        ++n5;
                    }
                    --n3;
                    continue block7;
                }
                default: {
                    ++n5;
                }
            }
        }
        switch (this.getLanguageIndex()) {
            case 1: {
                if (n4 <= n5) {
                    Segment segment3 = new Segment(segment.array, segment.getBeginIndex() + n4, n5 - n4);
                    this.addToken(this.groovyTokenMaker.getTokenList(segment3, n, n2 + n4));
                }
                this.addLevelToken(n3);
                break;
            }
            default: {
                if (n4 < n5) {
                    this.addToken(segment, segment.getBeginIndex() + n4, segment.getBeginIndex() + n5 - 1, 33, n2 + n4);
                }
                this.addNullToken();
            }
        }
        return this.firstToken;
    }

    protected void addLevelToken(int n) {
        this.addNullToken();
        this.currentToken.setType(-n);
    }

    protected int getInitialLevel(Segment segment, int n, int n2) {
        return n < 0 ? -n : 0;
    }

    protected void addToken(Token token) {
        Token token2 = token.getNextToken();
        if (this.firstToken == null) {
            this.currentToken = this.firstToken = (TokenImpl)token;
        } else {
            TokenImpl tokenImpl = (TokenImpl)token;
            this.currentToken.setNextToken((Token)tokenImpl);
            this.previousToken = this.currentToken;
            this.currentToken = tokenImpl;
        }
        this.currentToken.setLanguageIndex(this.getLanguageIndex());
        this.currentToken.setHyperlink(false);
        this.currentToken.setNextToken(null);
        if (token2 != null && token2.getType() != 0) {
            this.addToken(token2);
        }
    }

    public int getClosestStandardTokenTypeForInternalType(int n) {
        return n < 0 ? 0 : this.groovyTokenMaker.getClosestStandardTokenTypeForInternalType(n);
    }

    public boolean getCurlyBracesDenoteCodeBlocks(int n) {
        return true;
    }

    public String[] getLineCommentStartAndEnd(int n) {
        switch (n) {
            case 1: {
                return this.groovyTokenMaker.getLineCommentStartAndEnd(0);
            }
        }
        return null;
    }

    public boolean getShouldIndentNextLineAfter(Token token) {
        return this.groovyTokenMaker.getShouldIndentNextLineAfter(token);
    }

    private static class GroovyExpressionTokenMaker
    extends GroovyTokenMaker {
        private GroovyExpressionTokenMaker() {
        }

        protected void resetTokenList() {
            this.previousToken = null;
            this.currentToken = null;
            this.firstToken = null;
        }

        protected void reset() {
            super.resetTokenList();
        }
    }
}

