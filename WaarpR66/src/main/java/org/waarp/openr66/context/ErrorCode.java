/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.context;

import org.waarp.openr66.protocol.configuration.Messages;

/**
 * This enum class keeps all code that will be returned into the result and
 * store (char representation) into
 * the runner.
 */
public enum ErrorCode {
  /**
   * Code stands for initialization ok (internal connection, authentication)
   */
  InitOk('i'),
  /**
   * Code stands for pre processing ok
   */
  PreProcessingOk('B'),
  /**
   * Code stands for transfer OK
   */
  TransferOk('X'),
  /**
   * Code stands for post processing ok
   */
  PostProcessingOk('P'),
  /**
   * Code stands for All action are completed ok
   */
  CompleteOk('O'),
  /**
   * Code stands for connection is impossible (remote or local reason)
   */
  ConnectionImpossible('C'),
  /**
   * Code stands for connection is impossible now due to limits(remote or
   * local
   * reason)
   */
  ServerOverloaded('l'),
  /**
   * Code stands for bad authentication (remote or local)
   */
  BadAuthent('A'),
  /**
   * Code stands for External operation in error (pre, post or error
   * processing)
   */
  ExternalOp('E'),
  /**
   * Code stands for Transfer is in error
   */
  TransferError('T'),
  /**
   * Code stands for Transfer in error due to MD5
   */
  MD5Error('M'),
  /**
   * Code stands for Network disconnection
   */
  Disconnection('D'),
  /**
   * Code stands for Remote Shutdown
   */
  RemoteShutdown('r'),
  /**
   * Code stands for final action (like moving file) is in error
   */
  FinalOp('F'),
  /**
   * Code stands for unimplemented feature
   */
  Unimplemented('U'),
  /**
   * Code stands for shutdown is in progress
   */
  Shutdown('S'),
  /**
   * Code stands for a remote error is received
   */
  RemoteError('R'),
  /**
   * Code stands for an internal error
   */
  Internal('I'),
  /**
   * Code stands for a request of stopping transfer
   */
  StoppedTransfer('H'),
  /**
   * Code stands for a request of canceling transfer
   */
  CanceledTransfer('K'),
  /**
   * Warning in execution
   */
  Warning('W'),
  /**
   * Code stands for unknown type of error
   */
  Unknown('-'),
  /**
   * Code stands for a request that is already remotely finished
   */
  QueryAlreadyFinished('Q'),
  /**
   * Code stands for request that is still running
   */
  QueryStillRunning('s'),
  /**
   * Code stands for not known host
   */
  NotKnownHost('N'),
  /**
   * Code stands for self requested host starting request is invalid
   */
  LoopSelfRequestedHost('L'),
  /**
   * Code stands for request should exist but is not found on remote host
   */
  QueryRemotelyUnknown('u'),
  /**
   * Code stands for File not found error
   */
  FileNotFound('f'),
  /**
   * Code stands for Command not found error
   */
  CommandNotFound('c'),
  /**
   * Code stands for a request in PassThroughMode and required action is
   * incompatible with this mode
   */
  PassThroughMode('p'),
  /**
   * Code stands for running step
   */
  Running('z'),
  /**
   * Code stands for Incorrect command
   */
  IncorrectCommand('n'),
  /**
   * Code stands for File not allowed
   */
  FileNotAllowed('a'),
  /**
   * Code stands for Size not allowed
   */
  SizeNotAllowed('d');

  /**
   * Code could be used to switch case operations
   */
  public final char code;

  ErrorCode(char code) {
    this.code = code;
  }

  public String getCode() {
    return String.valueOf(code);
  }

  public String getMesg() {
    return Messages.getString("ErrorCode." + code);
  }

  /**
   * Code is either the 1 char code or the exact name in Enum
   *
   * @param code
   *
   * @return the ErrorCode according to the code
   */
  public static ErrorCode getFromCode(String code) {
    if (code.isEmpty()) {
      return Unknown;
    }
    switch (code.charAt(0)) {
      case 'i':
        return InitOk;
      case 'B':
        return PreProcessingOk;
      case 'P':
        return PostProcessingOk;
      case 'X':
        return TransferOk;
      case 'O':
        return CompleteOk;
      case 'C':
        return ConnectionImpossible;
      case 'A':
        return BadAuthent;
      case 'E':
        return ExternalOp;
      case 'T':
        return TransferError;
      case 'M':
        return MD5Error;
      case 'D':
        return Disconnection;
      case 'r':
        return RemoteShutdown;
      case 'F':
        return FinalOp;
      case 'U':
        return Unimplemented;
      case 'S':
        return Shutdown;
      case 'R':
        return RemoteError;
      case 'I':
        return Internal;
      case 'H':
        return StoppedTransfer;
      case 'K':
        return CanceledTransfer;
      case 'W':
        return Warning;
      case '-':
        return Unknown;
      case 'Q':
        return QueryAlreadyFinished;
      case 's':
        return QueryStillRunning;
      case 'N':
        return NotKnownHost;
      case 'L':
        return LoopSelfRequestedHost;
      case 'u':
        return QueryRemotelyUnknown;
      case 'f':
        return FileNotFound;
      case 'z':
        return Running;
      case 'c':
        return CommandNotFound;
      case 'p':
        return PassThroughMode;
      case 'l':
        return ServerOverloaded;
      case 'n':
        return IncorrectCommand;
      case 'a':
        return FileNotAllowed;
      case 'd':
        return SizeNotAllowed;
      default:
        ErrorCode ecode;
        try {
          ecode = valueOf(code.trim());
        } catch (final IllegalArgumentException e) {
          return Unknown;
        }
        return ecode;
    }
  }

  public static boolean isErrorCode(ErrorCode code) {
    switch (code) {
      case BadAuthent:
      case CanceledTransfer:
      case CommandNotFound:
      case ConnectionImpossible:
      case Disconnection:
      case ExternalOp:
      case FileNotFound:
      case FinalOp:
      case Internal:
      case LoopSelfRequestedHost:
      case MD5Error:
      case NotKnownHost:
      case PassThroughMode:
      case QueryAlreadyFinished:
      case QueryRemotelyUnknown:
      case QueryStillRunning:
      case RemoteError:
      case RemoteShutdown:
      case ServerOverloaded:
      case Shutdown:
      case StoppedTransfer:
      case TransferError:
      case Unimplemented:
      case IncorrectCommand:
      case FileNotAllowed:
      case SizeNotAllowed:
        return true;
      case CompleteOk:
      case InitOk:
      case PostProcessingOk:
      case PreProcessingOk:
      case Running:
      case TransferOk:
      case Unknown:
      case Warning:
        return false;
      default:
        break;
    }
    return true;
  }
}
