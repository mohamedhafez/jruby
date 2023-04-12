/******************************************************************************/
/* This file is generated by the bin/template script and should not be        */
/* modified manually. See                                                     */
/* bin/templates/jruby/org/yarp/Loader.java.erb                               */
/* if you are looking to modify the                                           */
/* template                                                                   */
/******************************************************************************/
package org.yarp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;

// GENERATED BY Loader.java.erb
// @formatter:off
public class Loader {
    private final byte[] source;
    private final String fileName;
    private final ByteBuffer buffer;
    private StaticScope staticScope = null;

    public static Nodes.Node load(String fileName, byte[] source, byte[] serialized) {
        return new Loader(fileName, source, serialized).load();
    }

    private Loader(String fileName, byte[] source, byte[] serialized) {
        this.fileName = fileName;
        this.source = source;
        this.buffer = ByteBuffer.wrap(serialized).order(ByteOrder.nativeOrder());    }

    private Nodes.Node load() {
        expect((byte) 'Y');
        expect((byte) 'A');
        expect((byte) 'R');
        expect((byte) 'P');

        expect((byte) 0);
        expect((byte) 4);
        expect((byte) 0);

        return loadNode();
    }

    // FIXME: this should be iso_8859_1 strings and not default charset.
    private String[] tokensToStrings(Nodes.Token[] tokens) {
        String[] strings = new String[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            Nodes.Token token = tokens[i];
            strings[i] = new String(source, token.startOffset, token.endOffset - token.startOffset);
        }

        return strings;
    }

    // FIXME: our staticscope assumes we know first offset to kwargs.
    private StaticScope loadStaticScope() {
        int type = buffer.get() & 0xFF;
        int startOffset = buffer.getInt();
        int endOffset = buffer.getInt();

        staticScope = StaticScopeFactory.newStaticScope(staticScope, StaticScope.Type.LOCAL, fileName, tokensToStrings(loadTokens()), -1);
                            
        return staticScope;
    }

    private byte[] loadString() {
        int length = buffer.getInt();
        byte[] string = new byte[length];
        buffer.get(string);
        return string;
    }

    private Nodes.Token loadOptionalToken() {
        int type = buffer.get() & 0xFF;
        if (type != 0) {
            return loadToken(type);
        } else {
            return null;
        }
    }

    private Nodes.Node loadOptionalNode() {
        int type = buffer.get() & 0xFF;
        if (type != 0) {
            return loadNode(type);
        } else {
            return null;
        }
    }

    private Nodes.Token[] loadTokens() {
        int length = buffer.getInt();
        Nodes.Token[] tokens = new Nodes.Token[length];
        for (int i = 0; i < length; i++) {
            tokens[i] = loadToken();
        }
        return tokens;
    }

    private Nodes.Node[] loadNodes() {
        int length = buffer.getInt();
        Nodes.Node[] nodes = new Nodes.Node[length];
        for (int i = 0; i < length; i++) {
            nodes[i] = loadNode();
        }
        return nodes;
    }

    private Nodes.Token loadToken() {
        return loadToken(buffer.get() & 0xFF);
    }

    private Nodes.Token loadToken(int type) {                            
        int startOffset = buffer.getInt();
        int endOffset = buffer.getInt();

        final Nodes.TokenType tokenType = Nodes.TOKEN_TYPES[type];
        return new Nodes.Token(tokenType, startOffset, endOffset);
    }

    private Nodes.Location loadLocation() {
        int startOffset = buffer.getInt();
        int endOffset = buffer.getInt();
        return new Nodes.Location(startOffset, endOffset);
    }

    private Nodes.Location loadOptionalLocation() {
        if (buffer.get() != 0) {
            return loadLocation();
        } else {
            return null;
        }
    }

    private int loadInteger() {
        return buffer.getInt();
    }

    private Nodes.Node loadAliasNode(int startOffset, int endOffset) {
        return new Nodes.AliasNode(loadNode(), loadNode(), loadLocation(), startOffset, endOffset);
    }
    private Nodes.Node loadAlternationPatternNode(int startOffset, int endOffset) {
        return new Nodes.AlternationPatternNode(loadNode(), loadNode(), loadLocation(), startOffset, endOffset);
    }
    private Nodes.Node loadAndNode(int startOffset, int endOffset) {
        return new Nodes.AndNode(loadNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadArgumentsNode(int startOffset, int endOffset) {
        return new Nodes.ArgumentsNode(loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadArrayNode(int startOffset, int endOffset) {
        return new Nodes.ArrayNode(loadNodes(), loadOptionalToken(), startOffset, endOffset);
    }
    private Nodes.Node loadArrayPatternNode(int startOffset, int endOffset) {
        return new Nodes.ArrayPatternNode(loadOptionalNode(), loadNodes(), loadOptionalNode(), loadNodes(), loadOptionalLocation(), loadOptionalLocation(), startOffset, endOffset);
    }
    private Nodes.Node loadAssocNode(int startOffset, int endOffset) {
        return new Nodes.AssocNode(loadNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadAssocSplatNode(int startOffset, int endOffset) {
        return new Nodes.AssocSplatNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadBeginNode(int startOffset, int endOffset) {
        return new Nodes.BeginNode(loadOptionalNode(), loadOptionalNode(), loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadBlockArgumentNode(int startOffset, int endOffset) {
        return new Nodes.BlockArgumentNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadBlockNode(int startOffset, int endOffset) {
        return new Nodes.BlockNode(loadStaticScope(), loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadBlockParameterNode(int startOffset, int endOffset) {
        return new Nodes.BlockParameterNode(loadOptionalToken(), startOffset, endOffset);
    }
    private Nodes.Node loadBlockParametersNode(int startOffset, int endOffset) {
        return new Nodes.BlockParametersNode(loadOptionalNode(), loadTokens(), loadOptionalLocation(), loadOptionalLocation(), startOffset, endOffset);
    }
    private Nodes.Node loadBreakNode(int startOffset, int endOffset) {
        return new Nodes.BreakNode(loadOptionalNode(), loadLocation(), startOffset, endOffset);
    }
    private Nodes.Node loadCallNode(int startOffset, int endOffset) {
        return new Nodes.CallNode(loadOptionalNode(), loadOptionalToken(), loadOptionalNode(), loadOptionalNode(), loadString(), startOffset, endOffset);
    }
    private Nodes.Node loadCapturePatternNode(int startOffset, int endOffset) {
        return new Nodes.CapturePatternNode(loadNode(), loadNode(), loadLocation(), startOffset, endOffset);
    }
    private Nodes.Node loadCaseNode(int startOffset, int endOffset) {
        return new Nodes.CaseNode(loadOptionalNode(), loadNodes(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadClassNode(int startOffset, int endOffset) {
        return new Nodes.ClassNode(loadStaticScope(), loadNode(), loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadClassVariableReadNode(int startOffset, int endOffset) {
        return new Nodes.ClassVariableReadNode(startOffset, endOffset);
    }
    private Nodes.Node loadClassVariableWriteNode(int startOffset, int endOffset) {
        return new Nodes.ClassVariableWriteNode(loadLocation(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadConstantPathNode(int startOffset, int endOffset) {
        return new Nodes.ConstantPathNode(loadOptionalNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadConstantPathWriteNode(int startOffset, int endOffset) {
        return new Nodes.ConstantPathWriteNode(loadNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadConstantReadNode(int startOffset, int endOffset) {
        return new Nodes.ConstantReadNode(startOffset, endOffset);
    }
    private Nodes.Node loadDefNode(int startOffset, int endOffset) {
        return new Nodes.DefNode(loadToken(), loadOptionalNode(), loadOptionalNode(), loadOptionalNode(), loadStaticScope(), startOffset, endOffset);
    }
    private Nodes.Node loadDefinedNode(int startOffset, int endOffset) {
        return new Nodes.DefinedNode(loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadElseNode(int startOffset, int endOffset) {
        return new Nodes.ElseNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadEnsureNode(int startOffset, int endOffset) {
        return new Nodes.EnsureNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadFalseNode(int startOffset, int endOffset) {
        return new Nodes.FalseNode(startOffset, endOffset);
    }
    private Nodes.Node loadFindPatternNode(int startOffset, int endOffset) {
        return new Nodes.FindPatternNode(loadOptionalNode(), loadNode(), loadNodes(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadFloatNode(int startOffset, int endOffset) {
        return new Nodes.FloatNode(startOffset, endOffset);
    }
    private Nodes.Node loadForNode(int startOffset, int endOffset) {
        return new Nodes.ForNode(loadNode(), loadNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadForwardingArgumentsNode(int startOffset, int endOffset) {
        return new Nodes.ForwardingArgumentsNode(startOffset, endOffset);
    }
    private Nodes.Node loadForwardingParameterNode(int startOffset, int endOffset) {
        return new Nodes.ForwardingParameterNode(startOffset, endOffset);
    }
    private Nodes.Node loadForwardingSuperNode(int startOffset, int endOffset) {
        return new Nodes.ForwardingSuperNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadGlobalVariableReadNode(int startOffset, int endOffset) {
        return new Nodes.GlobalVariableReadNode(loadToken(), startOffset, endOffset);
    }
    private Nodes.Node loadGlobalVariableWriteNode(int startOffset, int endOffset) {
        return new Nodes.GlobalVariableWriteNode(loadToken(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadHashNode(int startOffset, int endOffset) {
        return new Nodes.HashNode(loadOptionalToken(), loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadHashPatternNode(int startOffset, int endOffset) {
        return new Nodes.HashPatternNode(loadOptionalNode(), loadNodes(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadIfNode(int startOffset, int endOffset) {
        return new Nodes.IfNode(loadNode(), loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadImaginaryNode(int startOffset, int endOffset) {
        return new Nodes.ImaginaryNode(startOffset, endOffset);
    }
    private Nodes.Node loadInNode(int startOffset, int endOffset) {
        return new Nodes.InNode(loadNode(), loadOptionalNode(), loadLocation(), startOffset, endOffset);
    }
    private Nodes.Node loadInstanceVariableReadNode(int startOffset, int endOffset) {
        return new Nodes.InstanceVariableReadNode(startOffset, endOffset);
    }
    private Nodes.Node loadInstanceVariableWriteNode(int startOffset, int endOffset) {
        return new Nodes.InstanceVariableWriteNode(loadLocation(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadIntegerNode(int startOffset, int endOffset) {
        return new Nodes.IntegerNode(startOffset, endOffset);
    }
    private Nodes.Node loadInterpolatedRegularExpressionNode(int startOffset, int endOffset) {
        return new Nodes.InterpolatedRegularExpressionNode(loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadInterpolatedStringNode(int startOffset, int endOffset) {
        return new Nodes.InterpolatedStringNode(loadOptionalToken(), loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadInterpolatedSymbolNode(int startOffset, int endOffset) {
        return new Nodes.InterpolatedSymbolNode(loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadInterpolatedXStringNode(int startOffset, int endOffset) {
        return new Nodes.InterpolatedXStringNode(loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadKeywordParameterNode(int startOffset, int endOffset) {
        return new Nodes.KeywordParameterNode(loadToken(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadKeywordRestParameterNode(int startOffset, int endOffset) {
        return new Nodes.KeywordRestParameterNode(loadOptionalToken(), startOffset, endOffset);
    }
    private Nodes.Node loadLambdaNode(int startOffset, int endOffset) {
        return new Nodes.LambdaNode(loadStaticScope(), loadToken(), loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadLocalVariableReadNode(int startOffset, int endOffset) {
        return new Nodes.LocalVariableReadNode(loadInteger(), startOffset, endOffset);
    }
    private Nodes.Node loadLocalVariableWriteNode(int startOffset, int endOffset) {
        return new Nodes.LocalVariableWriteNode(loadLocation(), loadOptionalNode(), loadInteger(), startOffset, endOffset);
    }
    private Nodes.Node loadMatchPredicateNode(int startOffset, int endOffset) {
        return new Nodes.MatchPredicateNode(loadNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadMatchRequiredNode(int startOffset, int endOffset) {
        return new Nodes.MatchRequiredNode(loadNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadMissingNode(int startOffset, int endOffset) {
        return new Nodes.MissingNode(startOffset, endOffset);
    }
    private Nodes.Node loadModuleNode(int startOffset, int endOffset) {
        return new Nodes.ModuleNode(loadStaticScope(), loadNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadMultiWriteNode(int startOffset, int endOffset) {
        return new Nodes.MultiWriteNode(loadNodes(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadNextNode(int startOffset, int endOffset) {
        return new Nodes.NextNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadNilNode(int startOffset, int endOffset) {
        return new Nodes.NilNode(startOffset, endOffset);
    }
    private Nodes.Node loadNoKeywordsParameterNode(int startOffset, int endOffset) {
        return new Nodes.NoKeywordsParameterNode(startOffset, endOffset);
    }
    private Nodes.Node loadOperatorAndAssignmentNode(int startOffset, int endOffset) {
        return new Nodes.OperatorAndAssignmentNode(loadNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadOperatorAssignmentNode(int startOffset, int endOffset) {
        return new Nodes.OperatorAssignmentNode(loadNode(), loadToken(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadOperatorOrAssignmentNode(int startOffset, int endOffset) {
        return new Nodes.OperatorOrAssignmentNode(loadNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadOptionalParameterNode(int startOffset, int endOffset) {
        return new Nodes.OptionalParameterNode(loadToken(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadOrNode(int startOffset, int endOffset) {
        return new Nodes.OrNode(loadNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadParametersNode(int startOffset, int endOffset) {
        return new Nodes.ParametersNode(loadNodes(), loadNodes(), loadNodes(), loadOptionalNode(), loadNodes(), loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadParenthesesNode(int startOffset, int endOffset) {
        return new Nodes.ParenthesesNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadPinnedExpressionNode(int startOffset, int endOffset) {
        return new Nodes.PinnedExpressionNode(loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadPinnedVariableNode(int startOffset, int endOffset) {
        return new Nodes.PinnedVariableNode(loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadPostExecutionNode(int startOffset, int endOffset) {
        return new Nodes.PostExecutionNode(loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadPreExecutionNode(int startOffset, int endOffset) {
        return new Nodes.PreExecutionNode(loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadProgramNode(int startOffset, int endOffset) {
        return new Nodes.ProgramNode(loadStaticScope(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadRangeNode(int startOffset, int endOffset) {
        return new Nodes.RangeNode(loadOptionalNode(), loadOptionalNode(), loadLocation(), startOffset, endOffset);
    }
    private Nodes.Node loadRationalNode(int startOffset, int endOffset) {
        return new Nodes.RationalNode(startOffset, endOffset);
    }
    private Nodes.Node loadRedoNode(int startOffset, int endOffset) {
        return new Nodes.RedoNode(startOffset, endOffset);
    }
    private Nodes.Node loadRegularExpressionNode(int startOffset, int endOffset) {
        return new Nodes.RegularExpressionNode(loadToken(), loadToken(), loadString(), startOffset, endOffset);
    }
    private Nodes.Node loadRequiredDestructuredParameterNode(int startOffset, int endOffset) {
        return new Nodes.RequiredDestructuredParameterNode(loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadRequiredParameterNode(int startOffset, int endOffset) {
        return new Nodes.RequiredParameterNode(startOffset, endOffset);
    }
    private Nodes.Node loadRescueModifierNode(int startOffset, int endOffset) {
        return new Nodes.RescueModifierNode(loadNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadRescueNode(int startOffset, int endOffset) {
        return new Nodes.RescueNode(loadNodes(), loadOptionalNode(), loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadRestParameterNode(int startOffset, int endOffset) {
        return new Nodes.RestParameterNode(loadOptionalToken(), startOffset, endOffset);
    }
    private Nodes.Node loadRetryNode(int startOffset, int endOffset) {
        return new Nodes.RetryNode(startOffset, endOffset);
    }
    private Nodes.Node loadReturnNode(int startOffset, int endOffset) {
        return new Nodes.ReturnNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadScopeNode(int startOffset, int endOffset) {
        return new Nodes.ScopeNode(loadTokens(), startOffset, endOffset);
    }
    private Nodes.Node loadSelfNode(int startOffset, int endOffset) {
        return new Nodes.SelfNode(startOffset, endOffset);
    }
    private Nodes.Node loadSingletonClassNode(int startOffset, int endOffset) {
        return new Nodes.SingletonClassNode(loadStaticScope(), loadNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadSourceEncodingNode(int startOffset, int endOffset) {
        return new Nodes.SourceEncodingNode(startOffset, endOffset);
    }
    private Nodes.Node loadSourceFileNode(int startOffset, int endOffset) {
        return new Nodes.SourceFileNode(loadString(), startOffset, endOffset);
    }
    private Nodes.Node loadSourceLineNode(int startOffset, int endOffset) {
        return new Nodes.SourceLineNode(startOffset, endOffset);
    }
    private Nodes.Node loadSplatNode(int startOffset, int endOffset) {
        return new Nodes.SplatNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadStatementsNode(int startOffset, int endOffset) {
        return new Nodes.StatementsNode(loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadStringConcatNode(int startOffset, int endOffset) {
        return new Nodes.StringConcatNode(loadNode(), loadNode(), startOffset, endOffset);
    }
    private Nodes.Node loadStringInterpolatedNode(int startOffset, int endOffset) {
        return new Nodes.StringInterpolatedNode(loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadStringNode(int startOffset, int endOffset) {
        return new Nodes.StringNode(loadOptionalToken(), loadToken(), loadString(), startOffset, endOffset);
    }
    private Nodes.Node loadSuperNode(int startOffset, int endOffset) {
        return new Nodes.SuperNode(loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadSymbolNode(int startOffset, int endOffset) {
        return new Nodes.SymbolNode(loadToken(), loadString(), startOffset, endOffset);
    }
    private Nodes.Node loadTrueNode(int startOffset, int endOffset) {
        return new Nodes.TrueNode(startOffset, endOffset);
    }
    private Nodes.Node loadUndefNode(int startOffset, int endOffset) {
        return new Nodes.UndefNode(loadNodes(), startOffset, endOffset);
    }
    private Nodes.Node loadUnlessNode(int startOffset, int endOffset) {
        return new Nodes.UnlessNode(loadNode(), loadOptionalNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadUntilNode(int startOffset, int endOffset) {
        return new Nodes.UntilNode(loadToken(), loadNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadWhenNode(int startOffset, int endOffset) {
        return new Nodes.WhenNode(loadNodes(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadWhileNode(int startOffset, int endOffset) {
        return new Nodes.WhileNode(loadToken(), loadNode(), loadOptionalNode(), startOffset, endOffset);
    }
    private Nodes.Node loadXStringNode(int startOffset, int endOffset) {
        return new Nodes.XStringNode(loadToken(), loadString(), startOffset, endOffset);
    }
    private Nodes.Node loadYieldNode(int startOffset, int endOffset) {
        return new Nodes.YieldNode(loadOptionalToken(), loadOptionalNode(), loadOptionalToken(), startOffset, endOffset);
    }
                            

    private Nodes.Node loadNode() {
        return loadNode(buffer.get() & 0xFF);
    }

    private Nodes.Node loadNode(int type) {
        int startOffset = buffer.getInt();
        int endOffset = buffer.getInt();

        switch (type) {
        case 0: return null; // Optional*
        case 1: return loadAliasNode(startOffset, endOffset);
        case 2: return loadAlternationPatternNode(startOffset, endOffset);
        case 3: return loadAndNode(startOffset, endOffset);
        case 4: return loadArgumentsNode(startOffset, endOffset);
        case 5: return loadArrayNode(startOffset, endOffset);
        case 6: return loadArrayPatternNode(startOffset, endOffset);
        case 7: return loadAssocNode(startOffset, endOffset);
        case 8: return loadAssocSplatNode(startOffset, endOffset);
        case 9: return loadBeginNode(startOffset, endOffset);
        case 10: return loadBlockArgumentNode(startOffset, endOffset);
        case 11: return loadBlockNode(startOffset, endOffset);
        case 12: return loadBlockParameterNode(startOffset, endOffset);
        case 13: return loadBlockParametersNode(startOffset, endOffset);
        case 14: return loadBreakNode(startOffset, endOffset);
        case 15: return loadCallNode(startOffset, endOffset);
        case 16: return loadCapturePatternNode(startOffset, endOffset);
        case 17: return loadCaseNode(startOffset, endOffset);
        case 18: return loadClassNode(startOffset, endOffset);
        case 19: return loadClassVariableReadNode(startOffset, endOffset);
        case 20: return loadClassVariableWriteNode(startOffset, endOffset);
        case 21: return loadConstantPathNode(startOffset, endOffset);
        case 22: return loadConstantPathWriteNode(startOffset, endOffset);
        case 23: return loadConstantReadNode(startOffset, endOffset);
        case 24: return loadDefNode(startOffset, endOffset);
        case 25: return loadDefinedNode(startOffset, endOffset);
        case 26: return loadElseNode(startOffset, endOffset);
        case 27: return loadEnsureNode(startOffset, endOffset);
        case 28: return loadFalseNode(startOffset, endOffset);
        case 29: return loadFindPatternNode(startOffset, endOffset);
        case 30: return loadFloatNode(startOffset, endOffset);
        case 31: return loadForNode(startOffset, endOffset);
        case 32: return loadForwardingArgumentsNode(startOffset, endOffset);
        case 33: return loadForwardingParameterNode(startOffset, endOffset);
        case 34: return loadForwardingSuperNode(startOffset, endOffset);
        case 35: return loadGlobalVariableReadNode(startOffset, endOffset);
        case 36: return loadGlobalVariableWriteNode(startOffset, endOffset);
        case 37: return loadHashNode(startOffset, endOffset);
        case 38: return loadHashPatternNode(startOffset, endOffset);
        case 39: return loadIfNode(startOffset, endOffset);
        case 40: return loadImaginaryNode(startOffset, endOffset);
        case 41: return loadInNode(startOffset, endOffset);
        case 42: return loadInstanceVariableReadNode(startOffset, endOffset);
        case 43: return loadInstanceVariableWriteNode(startOffset, endOffset);
        case 44: return loadIntegerNode(startOffset, endOffset);
        case 45: return loadInterpolatedRegularExpressionNode(startOffset, endOffset);
        case 46: return loadInterpolatedStringNode(startOffset, endOffset);
        case 47: return loadInterpolatedSymbolNode(startOffset, endOffset);
        case 48: return loadInterpolatedXStringNode(startOffset, endOffset);
        case 49: return loadKeywordParameterNode(startOffset, endOffset);
        case 50: return loadKeywordRestParameterNode(startOffset, endOffset);
        case 51: return loadLambdaNode(startOffset, endOffset);
        case 52: return loadLocalVariableReadNode(startOffset, endOffset);
        case 53: return loadLocalVariableWriteNode(startOffset, endOffset);
        case 54: return loadMatchPredicateNode(startOffset, endOffset);
        case 55: return loadMatchRequiredNode(startOffset, endOffset);
        case 56: return loadMissingNode(startOffset, endOffset);
        case 57: return loadModuleNode(startOffset, endOffset);
        case 58: return loadMultiWriteNode(startOffset, endOffset);
        case 59: return loadNextNode(startOffset, endOffset);
        case 60: return loadNilNode(startOffset, endOffset);
        case 61: return loadNoKeywordsParameterNode(startOffset, endOffset);
        case 62: return loadOperatorAndAssignmentNode(startOffset, endOffset);
        case 63: return loadOperatorAssignmentNode(startOffset, endOffset);
        case 64: return loadOperatorOrAssignmentNode(startOffset, endOffset);
        case 65: return loadOptionalParameterNode(startOffset, endOffset);
        case 66: return loadOrNode(startOffset, endOffset);
        case 67: return loadParametersNode(startOffset, endOffset);
        case 68: return loadParenthesesNode(startOffset, endOffset);
        case 69: return loadPinnedExpressionNode(startOffset, endOffset);
        case 70: return loadPinnedVariableNode(startOffset, endOffset);
        case 71: return loadPostExecutionNode(startOffset, endOffset);
        case 72: return loadPreExecutionNode(startOffset, endOffset);
        case 73: return loadProgramNode(startOffset, endOffset);
        case 74: return loadRangeNode(startOffset, endOffset);
        case 75: return loadRationalNode(startOffset, endOffset);
        case 76: return loadRedoNode(startOffset, endOffset);
        case 77: return loadRegularExpressionNode(startOffset, endOffset);
        case 78: return loadRequiredDestructuredParameterNode(startOffset, endOffset);
        case 79: return loadRequiredParameterNode(startOffset, endOffset);
        case 80: return loadRescueModifierNode(startOffset, endOffset);
        case 81: return loadRescueNode(startOffset, endOffset);
        case 82: return loadRestParameterNode(startOffset, endOffset);
        case 83: return loadRetryNode(startOffset, endOffset);
        case 84: return loadReturnNode(startOffset, endOffset);
        case 85: return loadScopeNode(startOffset, endOffset);
        case 86: return loadSelfNode(startOffset, endOffset);
        case 87: return loadSingletonClassNode(startOffset, endOffset);
        case 88: return loadSourceEncodingNode(startOffset, endOffset);
        case 89: return loadSourceFileNode(startOffset, endOffset);
        case 90: return loadSourceLineNode(startOffset, endOffset);
        case 91: return loadSplatNode(startOffset, endOffset);
        case 92: return loadStatementsNode(startOffset, endOffset);
        case 93: return loadStringConcatNode(startOffset, endOffset);
        case 94: return loadStringInterpolatedNode(startOffset, endOffset);
        case 95: return loadStringNode(startOffset, endOffset);
        case 96: return loadSuperNode(startOffset, endOffset);
        case 97: return loadSymbolNode(startOffset, endOffset);
        case 98: return loadTrueNode(startOffset, endOffset);
        case 99: return loadUndefNode(startOffset, endOffset);
        case 100: return loadUnlessNode(startOffset, endOffset);
        case 101: return loadUntilNode(startOffset, endOffset);
        case 102: return loadWhenNode(startOffset, endOffset);
        case 103: return loadWhileNode(startOffset, endOffset);
        case 104: return loadXStringNode(startOffset, endOffset);
        case 105: return loadYieldNode(startOffset, endOffset);
        default:
            throw new Error("Unknown node type: " + type);
        }
    }

    private void expect(byte value) {
        byte b = buffer.get();
        if (b != value) {
            throw new Error("Expected " + value + " but was " + b + " at position " + buffer.position());
        }
    }

}
// @formatter:on